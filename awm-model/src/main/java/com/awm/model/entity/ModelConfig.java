package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_config")
public class ModelConfig {
    @TableId(type = IdType.ASSIGN_UUID)
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
