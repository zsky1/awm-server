package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {
    private String id;
    private String groupId;
    private String title;
    private String description;
    private String assignedAgentId;
    private String assignedAgentName;
    private String status;
    private Integer progress;
    private String parentTaskId;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
