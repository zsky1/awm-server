package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String groupId;
    private String title;
    private String description;
    private String assignedAgentId;
    private String status;
    private Integer progress;
    private String parentTaskId;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
