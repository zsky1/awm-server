package com.awm.service.task;

import com.awm.ai.client.LlmClient;
import com.awm.ai.model.*;
import com.awm.ai.prompt.PromptBuilder;
import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.ChatGroupMapper;
import com.awm.dal.mapper.ChatGroupMemberMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.dal.mapper.ModelConfigMapper;
import com.awm.model.entity.Agent;
import com.awm.model.entity.ChatGroup;
import com.awm.model.entity.ChatGroupMember;
import com.awm.model.entity.McpServer;
import com.awm.model.entity.Message;
import com.awm.model.entity.ModelConfig;
import com.awm.model.entity.Task;
import com.awm.model.vo.TaskVO;
import com.awm.service.agent.AgentService;
import com.awm.service.agent.ConfigLoader;
import com.awm.service.chat.DispatchTaskEvent;
import com.awm.service.chat.MessageService;
import com.awm.service.chat.SseManager;
import com.awm.service.mcp.ToolCallProxy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 任务调度服务 - 总管调度核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final AgentMapper agentMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final McpServerMapper mcpServerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final AgentService agentService;
    private final TaskService taskService;
    private final MessageService messageService;
    private final ConfigLoader configLoader;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ToolCallProxy toolCallProxy;
    private final SseManager sseManager;
    private final ObjectMapper objectMapper;

    /**
     * 监听任务调度事件
     */
    @EventListener
    public void onDispatchTaskEvent(DispatchTaskEvent event) {
        dispatchTask(event.getGroupId(), event.getMessage());
    }

    /**
     * 总管调度核心逻辑
     * 1. 获取群内总管 Agent
     * 2. 获取群内所有成员的技能和状态
     * 3. 构建 Prompt 让总管拆解任务
     * 4. 解析总管返回的任务分配结果
     * 5. 创建 Task 记录
     * 6. 通知被分配的 Agent 开始执行
     *
     * 注意：此方法不标注 @Transactional，因为 executeSubTask 中使用了异步 subscribe()
     * 数据库操作通过各 Service 的 @Transactional 方法保证事务性
     */
    public void dispatchTask(String groupId, String userMessage) {
        // 1. 获取群内总管 Agent
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getManagerId() == null) {
            log.warn("Group has no manager: {}", groupId);
            return;
        }

        Agent manager = agentMapper.selectById(group.getManagerId());
        if (manager == null) {
            log.warn("Manager agent not found: {}", group.getManagerId());
            return;
        }

        // 2. 获取群内所有成员的技能和状态（批量查询避免 N+1）
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId));

        // 批量查询所有成员 Agent
        Map<String, Agent> agentMap = new HashMap<>();
        if (!members.isEmpty()) {
            List<String> agentIds = members.stream().map(ChatGroupMember::getAgentId).toList();
            agentMapper.selectBatchIds(agentIds).forEach(a -> agentMap.put(a.getId(), a));
        }

        List<Map<String, Object>> memberSkills = new ArrayList<>();
        for (ChatGroupMember member : members) {
            Agent agent = agentMap.get(member.getAgentId());
            if (agent == null) continue;

            Map<String, Object> skillInfo = new HashMap<>();
            skillInfo.put("name", agent.getName());
            skillInfo.put("position", agent.getPosition());
            skillInfo.put("runtimeStatus", agent.getRuntimeStatus());

            // 提取技能标签
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = objectMapper.readValue(agent.getConfig(), Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> skills = (List<Map<String, Object>>) config.get("skills");
                if (skills != null) {
                    List<String> tags = skills.stream()
                            .map(s -> (String) s.get("tag"))
                            .filter(Objects::nonNull)
                            .toList();
                    skillInfo.put("skills", tags);
                }
            } catch (Exception e) {
                skillInfo.put("skills", List.of());
            }

            memberSkills.add(skillInfo);
        }

        // 3. 获取最近群聊历史（用于上下文注入，支持 P1-1）
        List<Message> recentHistory = messageService.getRecentMessages(groupId, 20);
        List<MessageItem> historyItems = recentHistory.stream()
                .map(m -> {
                    if ("user".equals(m.getSenderType())) {
                        return MessageItem.user(m.getContent());
                    } else {
                        return MessageItem.assistant(m.getContent());
                    }
                })
                .toList();

        // 4. 构建 Prompt 让总管拆解任务
        String dispatchPrompt = promptBuilder.buildDispatcherPrompt(
                group.getName(), memberSkills, userMessage, historyItems);

        // 定义 assign_task 函数
        Map<String, Object> assignTaskParams = new HashMap<>();
        assignTaskParams.put("type", "object");
        Map<String, Object> agentIdProp = Map.of("type", "string", "description", "被分配的 Agent 名称");
        Map<String, Object> titleProp = Map.of("type", "string", "description", "子任务标题");
        Map<String, Object> descProp = Map.of("type", "string", "description", "子任务详细描述");
        assignTaskParams.put("properties", Map.of(
                "agent_name", agentIdProp,
                "title", titleProp,
                "description", descProp
        ));
        assignTaskParams.put("required", List.of("agent_name", "title", "description"));

        ToolDefinition assignTool = new ToolDefinition(
                "assign_task",
                "将子任务分配给指定的 Agent",
                assignTaskParams
        );

        ChatRequest request = ChatRequest.of(
                resolveModelName(manager),
                dispatchPrompt,
                List.of(),
                resolveApiKey(manager),
                resolveBaseUrl(manager)
        );

        // 4. 调用 LLM 让总管拆解任务
        ChatResponse response = llmClient.chatWithTools(request, List.of(assignTool));

        // 5. 解析任务分配结果，创建 Task 记录
        if (response.hasToolCalls()) {
            // 创建父任务
            Task parentTask = new Task();
            parentTask.setGroupId(groupId);
            parentTask.setTitle(userMessage.length() > 200 ? userMessage.substring(0, 200) : userMessage);
            parentTask.setAssignedAgentId(manager.getId());
            parentTask.setStatus("in_progress");
            parentTask.setProgress(0);
            taskService.createTask(parentTask);

            for (ChatResponse.ToolCall toolCall : response.toolCalls()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> args = objectMapper.readValue(
                            toolCall.arguments(), new TypeReference<>() {});

                    String agentName = args.get("agent_name");
                    String title = args.get("title");
                    String description = args.get("description");

                    // 查找 Agent（使用已缓存的 agentMap）
                    Agent assignedAgent = null;
                    for (Map.Entry<String, Agent> entry : agentMap.entrySet()) {
                        if (entry.getValue().getName().equals(agentName)) {
                            assignedAgent = entry.getValue();
                            break;
                        }
                    }
                    if (assignedAgent == null) {
                        log.warn("Agent not found for name: {}", agentName);
                        continue;
                    }

                    // 创建子任务
                    Task subTask = new Task();
                    subTask.setGroupId(groupId);
                    subTask.setTitle(title);
                    subTask.setDescription(description);
                    subTask.setAssignedAgentId(assignedAgent.getId());
                    subTask.setParentTaskId(parentTask.getId());
                    subTask.setStatus("pending");
                    subTask.setProgress(0);
                    taskService.createTask(subTask);

                    // 先在群里发 @指派消息
                    String assignMsg = "@" + agentName + " 请处理以下任务：" + title + "\n" + (description != null ? description : "");
                    messageService.saveAgentMessageWithMetadata(
                            groupId, manager.getId(), manager.getName(),
                            assignMsg, "at_assign",
                            Map.of("mentionedAgentId", assignedAgent.getId(), "mentionedAgentName", agentName)
                    );

                    // 6. 通知 Agent 开始执行（异步）
                    executeSubTask(subTask.getId(), assignedAgent.getId());

                } catch (Exception e) {
                    log.error("Failed to parse tool call arguments", e);
                }
            }

            // 总管发布分配消息
            messageService.saveAgentMessage(groupId, manager.getId().toString(),
                    manager.getName(), "任务已拆解并分配，请各位开始执行。", "text");
        } else {
            // 无匹配角色 → 总管直接回复
            String fallbackContent = response.content() != null ? response.content()
                    : "目前团队还没有负责这个任务的员工，我来直接解答。";
            messageService.saveAgentMessageWithMetadata(
                    groupId, manager.getId(), manager.getName(),
                    fallbackContent, "text",
                    Map.of("fallback", true)
            );
        }
    }

    /**
     * Agent 执行子任务
     * 1. ConfigLoader 加载 Agent 配置
     * 2. 构建 ChatRequest（含 System Prompt + 工具列表 + 上下文）
     * 3. 调用 LlmClient.chatStream() 流式执行
     * 4. 通过 SSE 推送流式内容到群聊
     * 5. 处理工具调用（调用 ToolCallProxy）
     * 6. 更新 Task 状态和进度
     */
    public void executeSubTask(String taskId, String agentId) {
        // 更新任务状态 + Agent 运行时状态为忙碌
        taskService.updateTaskStatus(taskId, "in_progress");
        agentService.updateStatus(agentId, "busy");

        try {
            // 1. ConfigLoader 加载 Agent 配置
            ConfigLoader.AgentRuntime runtime = configLoader.loadAgentRuntime(agentId);

            // 2. 获取 Agent 实体
            Agent agent = runtime.agent();

            // 3. 构建 ChatRequest
            TaskVO taskVO = taskService.getTaskById(taskId);
            final String taskGroupId = taskVO.getGroupId(); // 缓存 groupId，避免反复查询

            List<MessageItem> messages = List.of(
                    MessageItem.user("请执行以下任务：" + taskVO.getDescription())
            );

            // 构建 Agent 可用的工具列表
            List<ToolDefinition> tools = runtime.tools().stream()
                    .map(t -> new ToolDefinition(
                            (String) t.get("name"),
                            (String) t.get("description"),
                            (Map<String, Object>) t.get("parameters")
                    ))
                    .toList();

            ChatRequest chatRequest = ChatRequest.of(
                    resolveModelName(agent),
                    runtime.systemPrompt(),
                    messages,
                    resolveApiKey(agent),
                    resolveBaseUrl(agent)
            );

            // 4. 流式执行
            StringBuilder fullContent = new StringBuilder();
            // 累积流式 tool_call 片段：key=toolCallId, value=[name, accumulatedArgs]
            Map<String, String[]> toolCallAccumulator = new HashMap<>();

            llmClient.chatStream(chatRequest)
                    .doOnNext(chunk -> {
                        // 推理内容转发（reasoning）
                        if (chunk.isReasoning() && chunk.reasoning() != null) {
                            sseManager.pushEventToGroup(taskGroupId, "agent_reasoning",
                                formatEventData(agent.getId(), agent.getName(), chunk.reasoning(), taskId));
                        }
                        if (chunk.content() != null) {
                            fullContent.append(chunk.content());
                            // 4. SSE 推送流式内容
                            sseManager.pushEventToGroup(
                                    taskGroupId,
                                    "agent_message",
                                    formatEventData(agent.getId(), agent.getName(), chunk.content(), taskId)
                            );
                        }
                        if (chunk.toolCall() != null) {
                            ChatResponse.ToolCall tc = chunk.toolCall();
                            String tcId = tc.id();
                            // OpenAI 流式 tool_calls: name 只在第一个 delta 出现，arguments 需要拼接
                            if (!toolCallAccumulator.containsKey(tcId) && tcId != null && !tcId.isEmpty()) {
                                toolCallAccumulator.put(tcId, new String[]{
                                        tc.name() != null ? tc.name() : "",
                                        tc.arguments() != null ? tc.arguments() : ""
                                });
                            } else if (tcId != null && !tcId.isEmpty()) {
                                // 追加 arguments 片段
                                String[] existing = toolCallAccumulator.get(tcId);
                                if (existing != null && tc.arguments() != null) {
                                    existing[1] += tc.arguments();
                                }
                            } else {
                                // 没有 id 的情况（部分模型不返回 id），直接处理
                                handleToolCall(taskId, taskGroupId, agentId, tc);
                            }
                        }
                    })
                    .doOnComplete(() -> {
                        // 处理所有累积的完整 tool_calls
                        for (Map.Entry<String, String[]> entry : toolCallAccumulator.entrySet()) {
                            String[] parts = entry.getValue();
                            ChatResponse.ToolCall completeToolCall = new ChatResponse.ToolCall(
                                    entry.getKey(), parts[0], parts[1]);
                            handleToolCall(taskId, taskGroupId, agentId, completeToolCall);
                        }

                        // 6. 更新 Task 状态 + Agent 运行时状态恢复空闲
                        taskService.updateTaskStatus(taskId, "completed");
                        taskService.updateTaskProgress(taskId, 100);
                        agentService.updateStatus(agentId, "idle");

                        // 保存完整消息
                        messageService.saveAgentMessage(
                                taskGroupId,
                                agentId.toString(),
                                agent.getName(),
                                fullContent.toString(),
                                "text"
                        );
                    })
                    .doOnError(e -> {
                        log.error("Agent task execution failed: taskId={}, agentId={}", taskId, agentId, e);
                        taskService.updateTaskStatus(taskId, "failed");
                        agentService.updateStatus(agentId, "error");
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Failed to execute sub task: {}", taskId, e);
            taskService.updateTaskStatus(taskId, "failed");
            agentService.updateStatus(agentId, "error");
        }
    }

    private void handleToolCall(String taskId, String groupId, String agentId, ChatResponse.ToolCall toolCall) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(
                    toolCall.arguments(), new TypeReference<>() {});

            // 查找工具所属的 MCP 服务器
            String result = toolCallProxy.executeTool(agentId, null, toolCall.name(), params);

            // 推送工具调用结果
            messageService.saveAgentMessage(
                    groupId,
                    agentId.toString(),
                    "Tool:" + toolCall.name(),
                    result,
                    "tool_call"
            );
        } catch (Exception e) {
            log.error("Tool call failed: {}", toolCall.name(), e);
        }
    }

    private String formatEventData(String agentId, String agentName, String content, String taskId) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("agentId", agentId);
            data.put("agentName", agentName);
            data.put("content", content);
            if (taskId != null) data.put("taskId", taskId);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 解析 Agent 的模型配置（优先 Agent.modelConfigId，回退到数据库默认配置）
     * @return ModelConfig 或 null（如果数据库没有配置）
     */
    private ModelConfig resolveModelConfig(Agent agent) {
        if (agent == null) return null;

        // 1. 优先使用 Agent 绑定的 modelConfigId
        if (agent.getModelConfigId() != null && !agent.getModelConfigId().isBlank()) {
            ModelConfig config = modelConfigMapper.selectById(agent.getModelConfigId());
            if (config != null) {
                log.info("Agent[{}] 使用绑定模型配置: model={}, baseUrl={}, apiKey={}****",
                        agent.getName(), config.getModel(), config.getBaseUrl(),
                        config.getApiKey() != null ? config.getApiKey().substring(Math.max(0, config.getApiKey().length() - 4)) : "null");
                return config;
            }
            log.warn("Agent {} 的 modelConfigId={} 在数据库中不存在，尝试使用默认配置", agent.getId(), agent.getModelConfigId());
        }

        // 2. 回退到数据库默认模型配置
        List<ModelConfig> defaults = modelConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ModelConfig>()
                        .eq(ModelConfig::getIsDefault, true));
        if (!defaults.isEmpty()) {
            ModelConfig config = defaults.get(0);
            log.info("Agent[{}] 使用默认模型配置: model={}, baseUrl={}, apiKey={}****",
                    agent.getName(), config.getModel(), config.getBaseUrl(),
                    config.getApiKey() != null ? config.getApiKey().substring(Math.max(0, config.getApiKey().length() - 4)) : "null");
            return config;
        }

        log.warn("Agent[{}] 未找到数据库模型配置，将回退到 application.yml", agent.getName());
        return null;
    }

    /**
     * 解析模型名称
     */
    private String resolveModelName(Agent agent) {
        ModelConfig config = resolveModelConfig(agent);
        if (config != null && config.getModel() != null && !config.getModel().isBlank()) {
            return config.getModel();
        }
        return null; // null → OpenAiLlmClient 回退到 application.yml
    }

    /**
     * 解析 API Key
     */
    private String resolveApiKey(Agent agent) {
        ModelConfig config = resolveModelConfig(agent);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return config.getApiKey();
        }
        return null; // null → OpenAiLlmClient 回退到 application.yml
    }

    /**
     * 解析 Base URL
     */
    private String resolveBaseUrl(Agent agent) {
        ModelConfig config = resolveModelConfig(agent);
        if (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            return config.getBaseUrl();
        }
        return null; // null → OpenAiLlmClient 回退到 application.yml
    }
}
