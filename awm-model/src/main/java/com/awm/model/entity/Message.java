package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String groupId;
    private String senderType;
    private String senderId;
    private String senderName;
    private String content;
    private String messageType;
    private String metadata;
    private LocalDateTime createdAt;
}
