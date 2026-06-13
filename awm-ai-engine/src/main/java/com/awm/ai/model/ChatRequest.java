package com.awm.ai.model;

import java.util.List;
import java.util.Map;

/**
 * LLM 对话请求
 */
public record ChatRequest(
    String model,
    String systemPrompt,
    List<MessageItem> messages,
    Map<String, Object> params
) {
    public static ChatRequest of(String model, String systemPrompt, List<MessageItem> messages) {
        return new ChatRequest(model, systemPrompt, messages, Map.of());
    }

    public static ChatRequest of(String model, String systemPrompt, List<MessageItem> messages, Map<String, Object> params) {
        return new ChatRequest(model, systemPrompt, messages, params);
    }
}
