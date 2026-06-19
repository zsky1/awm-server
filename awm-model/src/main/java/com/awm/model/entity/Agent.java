package com.awm.model.entity;

import com.awm.common.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "agent", autoResultMap = true)
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
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
