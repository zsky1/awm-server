package com.awm.service.mcp;

import com.awm.ai.client.McpClient;
import com.awm.dal.mapper.AgentMcpBindingMapper;
import com.awm.dal.mapper.McpCallLogMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.model.entity.AgentMcpBinding;
import com.awm.model.entity.McpCallLog;
import com.awm.model.entity.McpServer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具调用代理
 * 1. 检查 agent_mcp_binding 是否授权
 * 2. 调用 McpClient.callTool()
 * 3. 记录调用日志
 * 4. 返回结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallProxy {

    private final AgentMcpBindingMapper agentMcpBindingMapper;
    private final McpServerMapper mcpServerMapper;
    private final McpCallLogMapper mcpCallLogMapper;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    /**
     * 代理工具调用
     *
     * @param agentId     调用方 Agent ID
     * @param mcpServerId MCP 服务 ID（可为 null，自动查找）
     * @param toolName    工具名称
     * @param params      调用参数
     * @return 工具调用结果
     */
    public String executeTool(String agentId, String mcpServerId, String toolName, Map<String, Object> params) {
        // 1. 查找工具所属的 MCP 服务器
        if (mcpServerId == null) {
            mcpServerId = findMcpServerByToolName(toolName);
        }

        if (mcpServerId == null) {
            throw new RuntimeException("Tool not found: " + toolName);
        }

        // 2. 检查授权
        Long bindingCount = agentMcpBindingMapper.selectCount(
                new LambdaQueryWrapper<AgentMcpBinding>()
                        .eq(AgentMcpBinding::getAgentId, agentId)
                        .eq(AgentMcpBinding::getMcpServerId, mcpServerId)
                        .eq(AgentMcpBinding::getEnabled, true));

        if (bindingCount == 0) {
            throw new RuntimeException("Agent " + agentId + " is not authorized to use MCP server " + mcpServerId);
        }

        // 获取 MCP 服务器信息
        McpServer server = mcpServerMapper.selectById(mcpServerId);
        if (server == null) {
            throw new RuntimeException("MCP Server not found: " + mcpServerId);
        }

        // 3. 调用工具
        long startTime = System.currentTimeMillis();
        String status = "success";
        String resultStr;
        com.fasterxml.jackson.databind.JsonNode responseData = null;

        try {
            var result = mcpClient.callTool(server.getEndpoint(), toolName, params);
            responseData = result;
            resultStr = result != null ? result.toString() : "";
        } catch (Exception e) {
            status = "error";
            resultStr = e.getMessage();
            log.error("MCP tool call failed: {} on server {}", toolName, server.getName(), e);
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // 4. 记录调用日志
        McpCallLog callLog = new McpCallLog();
        callLog.setAgentId(agentId);
        callLog.setMcpServerId(mcpServerId);
        callLog.setToolName(toolName);
        callLog.setStatus(status);
        callLog.setDurationMs(durationMs);

        try {
            callLog.setRequestParams(objectMapper.writeValueAsString(params));
            callLog.setResponseData(responseData != null ? objectMapper.writeValueAsString(responseData) : null);
        } catch (Exception e) {
            log.warn("Failed to serialize call log params/response", e);
        }

        mcpCallLogMapper.insert(callLog);

        return resultStr;
    }

    /**
     * 根据工具名称查找所属的 MCP 服务器
     */
    private String findMcpServerByToolName(String toolName) {
        // 遍历所有 MCP 服务器，查找包含该工具的服务器
        List<McpServer> servers = mcpServerMapper.selectList(null);
        for (McpServer server : servers) {
            try {
                java.util.List<java.util.Map<String, Object>> tools = objectMapper.readValue(
                        server.getTools(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                for (Map<String, Object> tool : tools) {
                    if (toolName.equals(tool.get("name"))) {
                        return server.getId();
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return null;
    }
}
