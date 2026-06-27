package com.awm.ai.model;

/**
 * LLM 流式对话分块
 */
public record ChatChunk(
    String content,
    String reasoning,
    ChatResponse.ToolCall toolCall,
    boolean finished
) {

    public static ChatChunk text(String content) {
        return new ChatChunk(content, null, null, false);
    }

    public static ChatChunk reasoning(String reasoning) {
        return new ChatChunk(null, reasoning, null, false);
    }

    public static ChatChunk toolCall(ChatResponse.ToolCall toolCall) {
        return new ChatChunk(null, null, toolCall, false);
    }

    public static ChatChunk done() {
        return new ChatChunk(null, null, null, true);
    }

    public boolean isText() {
        return content != null && !content.isEmpty();
    }

    public boolean isReasoning() {
        return reasoning != null && !reasoning.isEmpty();
    }
}
