package com.awm.model.entity;

import com.awm.common.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "mcp_call_log", autoResultMap = true)
public class McpCallLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String mcpServerId;
    private String toolName;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String requestParams;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String responseData;
    private String status;
    private Long durationMs;
    private LocalDateTime createdAt;
}
