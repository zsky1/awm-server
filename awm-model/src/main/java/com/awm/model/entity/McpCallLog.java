package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mcp_call_log")
public class McpCallLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String mcpServerId;
    private String toolName;
    private String requestParams;
    private String responseData;
    private String status;
    private Long durationMs;
    private LocalDateTime createdAt;
}
