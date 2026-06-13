package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class McpServerCreateDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "端点不能为空")
    private String endpoint;
    private String description;
    private String config;
}
