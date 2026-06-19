package com.awm.model.entity;

import com.awm.common.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "mcp_server", autoResultMap = true)
public class McpServer {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String endpoint;
    private String description;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tools;
    private String healthStatus;
    private LocalDateTime lastCheckAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
