package com.awm.service.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接管理器
 * 管理 SSE 连接的创建、移除和消息推送
 */
@Slf4j
@Component
public class SseManager {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 分钟超时

    /**
     * groupId -> List<SseEmitter>
     */
    private final Map<String, List<SseEmitter>> groupEmitters = new ConcurrentHashMap<>();

    /**
     * 添加 SSE 连接
     */
    public SseEmitter addEmitter(String groupId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        groupEmitters.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(groupId, emitter));
        emitter.onTimeout(() -> removeEmitter(groupId, emitter));
        emitter.onError(e -> removeEmitter(groupId, emitter));

        log.info("SSE emitter added for group: {}", groupId);
        return emitter;
    }

    /**
     * 移除 SSE 连接
     */
    public void removeEmitter(String groupId, SseEmitter emitter) {
        List<SseEmitter> emitters = groupEmitters.get(groupId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                groupEmitters.remove(groupId);
            }
        }
        log.info("SSE emitter removed for group: {}", groupId);
    }

    /**
     * 推送消息到指定群的所有 SSE 连接
     */
    public void pushToGroup(String groupId, String data) {
        List<SseEmitter> emitters = groupEmitters.get(groupId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                log.warn("Failed to push SSE data, removing emitter", e);
                removeEmitter(groupId, emitter);
            }
        }
    }

    /**
     * 推送命名事件到指定群
     */
    public void pushEventToGroup(String groupId, String eventName, String data) {
        List<SseEmitter> emitters = groupEmitters.get(groupId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("Failed to push SSE event, removing emitter", e);
                removeEmitter(groupId, emitter);
            }
        }
    }

    /**
     * 获取群内活跃连接数
     */
    public int getConnectionCount(String groupId) {
        List<SseEmitter> emitters = groupEmitters.get(groupId);
        return emitters != null ? emitters.size() : 0;
    }
}
