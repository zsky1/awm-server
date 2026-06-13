package com.awm.ai.model;

import java.util.List;
import java.util.Map;

/**
 * LLM 对话响应
 */
public record ChatResponse(
    String content,
    List<ToolCall> toolCalls,
    Usage usage
) {

    public record ToolCall(
        String id,
        String name,
        String arguments
    ) {}

    public record Usage(
        int promptTokens,
        int completionTokens,
        int totalTokens
    ) {}

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
