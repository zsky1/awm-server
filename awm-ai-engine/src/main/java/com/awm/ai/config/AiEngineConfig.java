package com.awm.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI Engine 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiEngineConfig {

    private LlmConfig llm = new LlmConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class LlmConfig {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o";
        private double temperature = 0.7;
        private int maxTokens = 4096;
    }

    @Data
    public static class EmbeddingConfig {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "text-embedding-3-small";
        private int dimensions = 1536;
    }
}
