package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent")
public class Agent {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String avatar;
    private String position;
    private String departmentId;
    private String supervisorId;
    private String personaPrompt;
    private String lifecycleStatus;
    private String runtimeStatus;
    private String modelConfigId;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
