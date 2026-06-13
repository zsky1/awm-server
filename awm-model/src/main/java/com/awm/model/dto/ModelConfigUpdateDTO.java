package com.awm.model.dto;

import lombok.Data;

@Data
public class ModelConfigUpdateDTO {
    private String name;
    private String provider;
    private String model;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isDefault;
    private String description;
}
