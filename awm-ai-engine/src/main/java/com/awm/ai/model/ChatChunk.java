package com.awm.ai.model;

/**
 * LLM 流式对话分块
 */
public record ChatChunk(
    String content,
    ChatResponse.ToolCall toolCall,
    boolean finished
) {

    public static ChatChunk text(String content) {
        return new ChatChunk(content, null, false);
    }

    public static ChatChunk toolCall(ChatResponse.ToolCall toolCall) {
        return new ChatChunk(null, toolCall, false);
    }

    public static ChatChunk done() {
        return new ChatChunk(null, null, true);
    }
}
