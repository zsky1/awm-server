package com.awm.service.agent;

import com.awm.ai.client.McpClient;
import com.awm.ai.prompt.PromptBuilder;
import com.awm.dal.mapper.AgentKnowledgeBindingMapper;
import com.awm.dal.mapper.AgentMcpBindingMapper;
import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.model.entity.Agent;
import com.awm.model.entity.AgentKnowledgeBinding;
import com.awm.model.entity.AgentMcpBinding;
import com.awm.model.entity.McpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 运行时配置加载器
 * 加载 Agent 完整运行时配置，拼接 System Prompt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigLoader {

    private final AgentMapper agentMapper;
    private final AgentMcpBindingMapper agentMcpBindingMapper;
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;
    private final McpServerMapper mcpServerMapper;
    private final McpClient mcpClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 加载 Agent 完整运行时配置
     * <ol>
     *   <li>读取 Agent 基础信息和 personaPrompt</li>
     *   <li>解析 config JSONB</li>
     *   <li>查询 agent_mcp_binding 获取工具列表</li>
     *   <li>查询 agent_knowledge_binding 获取知识库列表</li>
     *   <li>拼接完整 System Prompt</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    public AgentRuntime loadAgentRuntime(String agentId) {
        // 1. 读取 Agent 基础信息
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + agentId);
        }

        // 2. 解析 config JSONB
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(agent.getConfig(), Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse agent config, using empty map", e);
            config = new HashMap<>();
        }

        // 3. 查询 MCP 工具绑定
        List<AgentMcpBinding> mcpBindings = agentMcpBindingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentMcpBinding>()
                        .eq(AgentMcpBinding::getAgentId, agentId)
                        .eq(AgentMcpBinding::getEnabled, true));

        List<Map<String, Object>> tools = new ArrayList<>();
        for (AgentMcpBinding binding : mcpBindings) {
            try {
                List<Map<String, Object>> serverTools = mcpClient.listTools(
                        getMcpEndpoint(binding.getMcpServerId()));
                tools.addAll(serverTools);
            } catch (Exception e) {
                log.warn("Failed to load tools from MCP server: {}", binding.getMcpServerId(), e);
            }
        }

        // 4. 查询知识库绑定
        List<AgentKnowledgeBinding> kbBindings = agentKnowledgeBindingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getAgentId, agentId));

        List<String> knowledgeBaseIds = kbBindings.stream()
                .map(AgentKnowledgeBinding::getKbId)
                .collect(Collectors.toList());

        // 5. 拼接完整 System Prompt
        List<Map<String, Object>> skills = config.get("skills") != null
                ? (List<Map<String, Object>>) config.get("skills")
                : List.of();
        List<Map<String, Object>> rules = config.get("rules") != null
                ? (List<Map<String, Object>>) config.get("rules")
                : List.of();

        String systemPrompt = promptBuilder.buildSystemPrompt(agent, config, skills, rules, null);

        // 6. 返回 AgentRuntime 对象
        return new AgentRuntime(agent, config, systemPrompt, tools, knowledgeBaseIds);
    }

    private String getMcpEndpoint(String mcpServerId) {
        McpServer server = mcpServerMapper.selectById(mcpServerId);
        if (server == null) {
            log.warn("MCP Server not found: {}", mcpServerId);
            return "";
        }
        return server.getEndpoint();
    }

    /**
     * Agent 运行时对象
     */
    public record AgentRuntime(
            Agent agent,
            Map<String, Object> config,
            String systemPrompt,
            List<Map<String, Object>> tools,
            List<String> knowledgeBaseIds
    ) {}
}
