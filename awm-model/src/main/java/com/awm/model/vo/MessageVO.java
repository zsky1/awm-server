package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
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
