package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mcp_server")
public class McpServer {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String endpoint;
    private String description;
    private String tools;
    private String healthStatus;
    private LocalDateTime lastCheckAt;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
