package com.awm.ai.prompt;

import com.awm.model.entity.Agent;
import org.springframework.stereotype.Component;

import com.awm.ai.model.MessageItem;

import java.util.List;
import java.util.Map;

/**
 * System Prompt 构建器
 */
@Component
public class PromptBuilder {

    /**
     * 构建完整 System Prompt
     *
     * @param agent         Agent 实体
     * @param config        Agent 配置（从 config JSONB 解析）
     * @param skills        技能列表
     * @param rules         规则列表
     * @param memorySummary 长期记忆摘要（可为 null）
     * @return 完整 System Prompt
     */
    @SuppressWarnings("unchecked")
    public String buildSystemPrompt(Agent agent, Map<String, Object> config,
                                     List<Map<String, Object>> skills,
                                     List<Map<String, Object>> rules,
                                     String memorySummary) {
        StringBuilder sb = new StringBuilder();

        // 角色定义
        sb.append("你是").append(agent.getName());
        if (agent.getPosition() != null) {
            sb.append("，").append(agent.getPosition());
        }
        sb.append("。\n\n");

        // 人设
        if (agent.getPersonaPrompt() != null && !agent.getPersonaPrompt().isBlank()) {
            sb.append("## 人设\n");
            sb.append(agent.getPersonaPrompt()).append("\n\n");
        }

        // 技能
        if (skills != null && !skills.isEmpty()) {
            sb.append("## 你擅长以下技能\n");
            for (Map<String, Object> skill : skills) {
                String tag = (String) skill.getOrDefault("tag", "");
                String desc = (String) skill.getOrDefault("description", "");
                sb.append("- ").append(tag);
                if (!desc.isEmpty()) {
                    sb.append(": ").append(desc);
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 规则
        if (rules != null && !rules.isEmpty()) {
            sb.append("## 请严格遵守以下规则\n");
            for (Map<String, Object> rule : rules) {
                String content = (String) rule.getOrDefault("content", "");
                sb.append("- ").append(content).append("\n");
            }
            sb.append("\n");
        }

        // 长期记忆摘要
        if (memorySummary != null && !memorySummary.isBlank()) {
            sb.append("## 你之前的关键经验\n");
            sb.append(memorySummary).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 构建总管调度 Prompt
     *
     * @param groupName     群名称
     * @param memberSkills  群成员及技能列表
     * @param userTask      用户任务内容
     * @param chatHistory   最近群聊上下文（可为空）
     * @return 调度 Prompt
     */
    public String buildDispatcherPrompt(String groupName, List<Map<String, Object>> memberSkills,
                                         String userTask, List<MessageItem> chatHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是群聊「").append(groupName).append("」的总裁助理/秘书。\n\n");
        sb.append("你的职责是时刻服务总裁：\n");
        sb.append("- 收到任何消息都要第一时间响应，不需要等总裁 @ 你\n");
        sb.append("- 简单问候或闲聊时，直接友好回复\n");
        sb.append("- 简单问题直接解答，复杂任务拆解分派给对应员工\n");
        sb.append("- 记住上下文，同样的问题不要重复问，不要复述历史对话内容\n");
        sb.append("- 任务完成后总结归档，必要时主动汇报进度\n\n");
        sb.append("## 群成员及其技能\n");
        for (Map<String, Object> member : memberSkills) {
            String name = (String) member.getOrDefault("name", "");
            String position = (String) member.getOrDefault("position", "");
            List<String> skillTags = (List<String>) member.getOrDefault("skills", List.of());
            sb.append("- ").append(name);
            if (position != null && !position.isEmpty()) {
                sb.append(" (").append(position).append(")");
            }
            if (!skillTags.isEmpty()) {
                sb.append(" [").append(String.join(", ", skillTags)).append("]");
            }
            sb.append("\n");
        }
        sb.append("\n## 用户任务\n");
        sb.append(userTask).append("\n\n");

        // 追加最近群聊上下文
        if (chatHistory != null && !chatHistory.isEmpty()) {
            sb.append("## 最近群聊上下文\n");
            int maxHistory = Math.min(chatHistory.size(), 20); // 最多显示最近20条
            List<MessageItem> recent = chatHistory.subList(
                    chatHistory.size() - maxHistory, chatHistory.size());
            for (MessageItem item : recent) {
                String roleLabel = switch (item.role()) {
                    case "user" -> "用户";
                    case "assistant" -> "AI";
                    default -> item.role();
                };
                sb.append("- [").append(roleLabel).append("]: ").append(item.content()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("请根据以上内容和上下文，决定如何响应：\n");
        sb.append("- 如果是简单对话或可自己解答的问题，直接在 content 中回复，不要调用 assign_task\n");
        sb.append("- 如果需要指派员工处理的任务，使用 assign_task 函数分配\n");
        sb.append("- 如果没有合适的员工，直接回答并告知总裁\n");
        return sb.toString();
    }
}
