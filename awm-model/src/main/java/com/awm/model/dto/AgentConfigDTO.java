package com.awm.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentConfigDTO {
    private List<SkillItem> skills;
    private List<RuleItem> rules;
    private MemoryConfig memoryConfig;
    private List<String> mcpBindingIds;
    private List<String> knowledgeBindingIds;

    @Data
    public static class SkillItem {
        private String tag;
        private String description;
        private String level;
        private String boundTool;
    }

    @Data
    public static class RuleItem {
        private String type;
        private String content;
        private Integer priority;
    }

    @Data
    public static class MemoryConfig {
        private ShortTermConfig shortTerm;
        private LongTermConfig longTerm;
    }

    @Data
    public static class ShortTermConfig {
        private Integer maxMessages;
        private Integer maxTokens;
    }

    @Data
    public static class LongTermConfig {
        private Boolean enabled;
        private String summaryInterval;
    }
}
