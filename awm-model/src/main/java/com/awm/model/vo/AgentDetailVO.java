package com.awm.model.vo;

import com.awm.model.dto.AgentConfigDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentDetailVO extends AgentVO {
    private String personaPrompt;
    private String departmentName;
    private String supervisorName;
    private String modelConfigId;
    private String modelName;
    private List<McpServerVO> mcpBindings;
    private List<AgentConfigDTO.RuleItem> rules;
    private AgentConfigDTO.MemoryConfig memoryConfig;
    private List<KnowledgeBaseVO> knowledgeBases;
    private TaskVO currentTask;
}
