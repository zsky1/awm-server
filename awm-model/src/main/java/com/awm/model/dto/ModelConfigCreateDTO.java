package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigCreateDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "提供商不能为空")
    private String provider;
    @NotBlank(message = "模型名称不能为空")
    private String model;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isDefault;
    private String description;
}
