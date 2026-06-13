package com.awm.ai.client;

import com.awm.ai.config.AiEngineConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Embedding API 客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private final AiEngineConfig aiEngineConfig;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    /**
     * 调用 Embedding API，返回向量数组
     */
    public float[] embed(String text) {
        String url = aiEngineConfig.getEmbedding().getBaseUrl() + "/embeddings";

        try {
            ObjectMapper mapper = objectMapper;
            String requestBody = mapper.writeValueAsString(Map.of(
                    "model", aiEngineConfig.getEmbedding().getModel(),
                    "input", text
            ));

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + aiEngineConfig.getEmbedding().getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    throw new RuntimeException("Embedding API call failed: " + response.code() + " - " + errorBody);
                }
                String responseBody = response.body().string();
                JsonNode root = mapper.readTree(responseBody);
                JsonNode embeddingNode = root.get("data").get(0).get("embedding");

                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                return vector;
            }
        } catch (IOException e) {
            throw new RuntimeException("Embedding API call error", e);
        }
    }
}
