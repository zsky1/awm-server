package com.awm.ai.client;

import com.awm.ai.model.ChatChunk;
import com.awm.ai.model.ChatRequest;
import com.awm.ai.model.ChatResponse;
import com.awm.ai.model.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 调用客户端接口
 */
public interface LlmClient {

    /**
     * 同步对话
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式对话（SSE）
     */
    Flux<ChatChunk> chatStream(ChatRequest request);

    /**
     * 带 Function Calling 的对话
     */
    ChatResponse chatWithTools(ChatRequest request, List<ToolDefinition> tools);
}
