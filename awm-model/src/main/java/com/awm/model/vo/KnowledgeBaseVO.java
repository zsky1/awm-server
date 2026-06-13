package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {
    private String id;
    private String name;
    private String description;
    private String alias;
    private String vectorCollection;
    private String indexStatus;
    private LocalDateTime lastIndexedAt;
}
