package com.awm.ai.model;

import java.util.List;
import java.util.Map;

/**
 * LLM 对话请求
 *
 * @param model       模型名称（如 deepseek-chat），为 null 时由 LlmClient 使用默认值
 * @param systemPrompt 系统提示词
 * @param messages    对话历史
 * @param params      额外参数（temperature, max_tokens 等）
 * @param apiKey      API Key，为 null 时 LlmClient 回退到 application.yml 配置
 * @param baseUrl     API Base URL（不含 /chat/completions），为 null 时 LlmClient 回退到 application.yml 配置
 */
public record ChatRequest(
    String model,
    String systemPrompt,
    List<MessageItem> messages,
    Map<String, Object> params,
    String apiKey,
    String baseUrl
) {
    public static ChatRequest of(String model, String systemPrompt, List<MessageItem> messages) {
        return new ChatRequest(model, systemPrompt, messages, Map.of(), null, null);
    }

    public static ChatRequest of(String model, String systemPrompt, List<MessageItem> messages, Map<String, Object> params) {
        return new ChatRequest(model, systemPrompt, messages, params, null, null);
    }

    /**
     * 带模型配置的工厂方法（apiKey / baseUrl 从数据库 model_config 加载）
     */
    public static ChatRequest of(String model, String systemPrompt, List<MessageItem> messages,
                                  String apiKey, String baseUrl) {
        return new ChatRequest(model, systemPrompt, messages, Map.of(), apiKey, baseUrl);
    }
}
