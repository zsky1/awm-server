package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentCreateDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String avatar;
    @NotBlank(message = "职位不能为空")
    private String position;
    @NotBlank(message = "部门不能为空")
    private String departmentId;
    private String supervisorId;
    private String personaPrompt;
    private String modelConfigId;
}
