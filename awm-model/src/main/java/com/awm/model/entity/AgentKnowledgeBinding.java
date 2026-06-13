package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_knowledge_binding")
public class AgentKnowledgeBinding {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String kbId;
    private String alias;
    private Integer syncInterval;
    private LocalDateTime createdAt;
}
