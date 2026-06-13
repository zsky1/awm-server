package com.awm.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MCP JSON-RPC 2.0 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpClient {

    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String JSONRPC_VERSION = "2.0";

    /**
     * MCP 初始化握手
     */
    public JsonNode initialize(String endpoint) {
        ObjectNode params = objectMapper.createObjectNode();
        ObjectNode capabilities = params.putObject("capabilities");
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "awm-server");
        clientInfo.put("version", "1.0.0");

        JsonNode result = callJsonRpc(endpoint, "initialize", params, 1);
        // 发送 initialized 通知
        callJsonRpc(endpoint, "notifications/initialized", null, 0);
        return result;
    }

    /**
     * 获取 MCP 服务提供的工具列表
     */
    public List<Map<String, Object>> listTools(String endpoint) {
        JsonNode result = callJsonRpc(endpoint, "tools/list", null, 2);
        List<Map<String, Object>> tools = new ArrayList<>();
        if (result != null && result.has("tools")) {
            for (JsonNode tool : result.get("tools")) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> toolMap = objectMapper.treeToValue(tool, Map.class);
                    tools.add(toolMap);
                } catch (Exception e) {
                    log.warn("Failed to parse tool: {}", tool, e);
                }
            }
        }
        return tools;
    }

    /**
     * 调用 MCP 工具
     */
    public JsonNode callTool(String endpoint, String toolName, Map<String, Object> arguments) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        if (arguments != null) {
            params.set("arguments", objectMapper.valueToTree(arguments));
        }
        return callJsonRpc(endpoint, "tools/call", params, 3);
    }

    private JsonNode callJsonRpc(String endpoint, String method, ObjectNode params, int id) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", JSONRPC_VERSION);
        request.put("method", method);
        if (params != null) {
            request.set("params", params);
        }
        if (id > 0) {
            request.put("id", id);
        }

        String url = endpoint;
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        // MCP 标准端点通常为 /mcp 或根路径
        if (!url.contains("/mcp")) {
            url = url + "mcp";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(request.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown";
                throw new RuntimeException("MCP call failed: " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                JsonNode error = root.get("error");
                throw new RuntimeException("MCP error: " + error.get("message").asText());
            }

            return root.get("result");
        } catch (IOException e) {
            throw new RuntimeException("MCP call error: " + method, e);
        }
    }
}
