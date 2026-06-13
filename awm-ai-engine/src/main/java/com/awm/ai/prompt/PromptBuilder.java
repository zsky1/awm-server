package com.awm.ai.prompt;

import com.awm.model.entity.Agent;
import org.springframework.stereotype.Component;

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
     */
    public String buildDispatcherPrompt(String groupName, List<Map<String, Object>> memberSkills, String userTask) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是群聊「").append(groupName).append("」的任务总管。\n\n");
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
        sb.append("请将以上任务拆解为子任务，并分配给最合适的群成员。");
        sb.append("使用 assign_task 函数为每个子任务指定执行人和描述。\n");
        return sb.toString();
    }
}
