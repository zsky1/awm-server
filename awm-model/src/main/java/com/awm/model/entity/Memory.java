package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("memory")
public class Memory {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String type;
    private String content;
    private String summary;
    private Integer tokenCount;
    private String sessionId;
    private LocalDateTime createdAt;
}
