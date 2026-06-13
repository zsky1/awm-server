package com.awm.ai.client;

import com.awm.ai.config.AiEngineConfig;
import com.awm.ai.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 API 的 LLM 客户端实现
 * 支持 DeepSeek / 通义千问 / OpenAI 等兼容接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final AiEngineConfig aiEngineConfig;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @Override
    public ChatResponse chat(ChatRequest request) {
        ObjectNode requestBody = buildRequestBody(request, false, null);
        String url = aiEngineConfig.getLlm().getBaseUrl() + "/chat/completions";

        OkHttpClient client = buildHttpClient();
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiEngineConfig.getLlm().getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown";
                throw new RuntimeException("LLM API call failed: " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body().string();
            return parseChatResponse(responseBody);
        } catch (IOException e) {
            throw new RuntimeException("LLM API call error", e);
        }
    }

    @Override
    public Flux<ChatChunk> chatStream(ChatRequest request) {
        ObjectNode requestBody = buildRequestBody(request, true, null);
        String url = aiEngineConfig.getLlm().getBaseUrl() + "/chat/completions";

        return Flux.create((FluxSink<ChatChunk> sink) -> {
            OkHttpClient client = buildHttpClient();
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + aiEngineConfig.getLlm().getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                    .build();

            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sink.error(new RuntimeException("LLM stream call error", e));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            sink.error(new RuntimeException("LLM stream call failed: " + response.code()));
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    sink.next(ChatChunk.done());
                                    break;
                                }
                                try {
                                    JsonNode chunk = objectMapper.readTree(data);
                                    JsonNode choices = chunk.get("choices");
                                    if (choices != null && choices.isArray() && !choices.isEmpty()) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null) {
                                            JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull()) {
                                                sink.next(ChatChunk.text(contentNode.asText()));
                                            }
                                            JsonNode toolCallsNode = delta.get("tool_calls");
                                            if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                                                JsonNode tc = toolCallsNode.get(0);
                                                String tcId = tc.has("id") ? tc.get("id").asText() : "";
                                                JsonNode fn = tc.get("function");
                                                String name = fn != null && fn.has("name") ? fn.get("name").asText() : "";
                                                String args = fn != null && fn.has("arguments") ? fn.get("arguments").asText() : "";
                                                sink.next(ChatChunk.toolCall(new ChatResponse.ToolCall(tcId, name, args)));
                                            }
                                        }
                                        JsonNode finishReason = choices.get(0).get("finish_reason");
                                        if (finishReason != null && "stop".equals(finishReason.asText())) {
                                            sink.next(ChatChunk.done());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to parse SSE chunk: {}", data, e);
                                }
                            }
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }
            });
        });
    }

    @Override
    public ChatResponse chatWithTools(ChatRequest request, List<ToolDefinition> tools) {
        ObjectNode requestBody = buildRequestBody(request, false, tools);
        String url = aiEngineConfig.getLlm().getBaseUrl() + "/chat/completions";

        OkHttpClient client = buildHttpClient();
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiEngineConfig.getLlm().getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown";
                throw new RuntimeException("LLM API call failed: " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body().string();
            return parseChatResponse(responseBody);
        } catch (IOException e) {
            throw new RuntimeException("LLM API call error", e);
        }
    }

    private ObjectNode buildRequestBody(ChatRequest request, boolean stream, List<ToolDefinition> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model() != null ? request.model() : aiEngineConfig.getLlm().getModel());
        body.put("stream", stream);

        ArrayNode messages = body.putArray("messages");
        if (request.systemPrompt() != null) {
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", request.systemPrompt());
        }
        if (request.messages() != null) {
            for (MessageItem item : request.messages()) {
                ObjectNode msg = messages.addObject();
                msg.put("role", item.role());
                msg.put("content", item.content());
            }
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsNode = body.putArray("tools");
            for (ToolDefinition tool : tools) {
                ObjectNode toolNode = toolsNode.addObject();
                ObjectNode functionNode = toolNode.putObject("function");
                functionNode.put("name", tool.name());
                functionNode.put("description", tool.description());
                if (tool.parameters() != null) {
                    functionNode.set("parameters", objectMapper.valueToTree(tool.parameters()));
                }
                toolNode.put("type", "function");
            }
        }

        if (request.params() != null) {
            request.params().forEach((key, value) -> {
                if (value instanceof Number) {
                    body.put(key, ((Number) value).doubleValue());
                } else if (value instanceof Boolean) {
                    body.put(key, (Boolean) value);
                } else if (value instanceof String) {
                    body.put(key, (String) value);
                }
            });
        }

        return body;
    }

    private ChatResponse parseChatResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            String content = null;
            List<ChatResponse.ToolCall> toolCalls = null;

            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode contentNode = message.get("content");
                    if (contentNode != null && !contentNode.isNull()) {
                        content = contentNode.asText();
                    }
                    JsonNode toolCallsNode = message.get("tool_calls");
                    if (toolCallsNode != null && toolCallsNode.isArray()) {
                        toolCalls = new ArrayList<>();
                        for (JsonNode tc : toolCallsNode) {
                            String id = tc.get("id").asText();
                            JsonNode fn = tc.get("function");
                            String name = fn.get("name").asText();
                            String args = fn.get("arguments").asText();
                            toolCalls.add(new ChatResponse.ToolCall(id, name, args));
                        }
                    }
                }
            }

            ChatResponse.Usage usage = null;
            JsonNode usageNode = root.get("usage");
            if (usageNode != null) {
                usage = new ChatResponse.Usage(
                        usageNode.get("prompt_tokens").asInt(),
                        usageNode.get("completion_tokens").asInt(),
                        usageNode.get("total_tokens").asInt()
                );
            }

            return new ChatResponse(content, toolCalls, usage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
