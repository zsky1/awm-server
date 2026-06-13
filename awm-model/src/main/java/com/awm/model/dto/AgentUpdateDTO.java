package com.awm.model.dto;

import lombok.Data;

@Data
public class AgentUpdateDTO {
    private String name;
    private String avatar;
    private String position;
    private String departmentId;
    private String supervisorId;
    private String personaPrompt;
    private String modelConfigId;
}
