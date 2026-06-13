package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModelConfigVO {
    private String id;
    private String name;
    private String provider;
    private String model;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isDefault;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
