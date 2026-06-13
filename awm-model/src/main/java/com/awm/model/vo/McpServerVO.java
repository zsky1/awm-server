package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class McpServerVO {
    private String id;
    private String name;
    private String endpoint;
    private String description;
    private String tools;
    private String healthStatus;
    private LocalDateTime lastCheckAt;
}
