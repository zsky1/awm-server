package com.awm.model.entity;

import com.awm.common.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "agent_mcp_binding", autoResultMap = true)
public class AgentMcpBinding {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String mcpServerId;
    private Boolean enabled;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String config;
    private LocalDateTime createdAt;
}
