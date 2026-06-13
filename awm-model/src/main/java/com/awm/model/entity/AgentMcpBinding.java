package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_mcp_binding")
public class AgentMcpBinding {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String mcpServerId;
    private Boolean enabled;
    private String config;
    private LocalDateTime createdAt;
}
