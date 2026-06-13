package com.awm.service.chat;

import com.awm.dal.mapper.MessageMapper;
import com.awm.model.entity.Message;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final SseManager sseManager;
    private final ObjectMapper objectMapper;

    /**
     * 保存消息
     */
    public Message saveMessage(String groupId, String senderType, String senderId,
                                String senderName, String content, String messageType,
                                Map<String, Object> metadata) {
        Message message = new Message();
        message.setGroupId(groupId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");

        if (metadata != null) {
            try {
                message.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                message.setMetadata("{}");
            }
        } else {
            message.setMetadata("{}");
        }

        messageMapper.insert(message);

        // 推送到群内 SSE 连接
        try {
            String data = objectMapper.writeValueAsString(message);
            sseManager.pushToGroup(groupId, data);
        } catch (Exception e) {
            log.warn("Failed to push message to SSE for group: {}", groupId, e);
        }

        return message;
    }

    /**
     * 保存 Agent 流式消息片段（追加内容）
     */
    public Message saveAgentMessage(String groupId, String agentId, String agentName,
                                     String content, String messageType) {
        return saveMessage(groupId, "agent", agentId, agentName, content, messageType, null);
    }

    /**
     * 分页查询消息
     */
    public IPage<Message> pageMessages(String groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getGroupId, groupId)
                .orderByDesc(Message::getCreatedAt);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 查询消息
     */
    public Message getMessageById(String id) {
        return messageMapper.selectById(id);
    }
}
