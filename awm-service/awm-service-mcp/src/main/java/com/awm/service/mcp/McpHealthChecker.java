package com.awm.service.mcp;

import com.awm.ai.client.McpClient;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.model.entity.McpServer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MCP 服务健康检查器
 * 定时检查所有 MCP 服务健康状态，更新 mcp_server.health_status 和 last_check_at
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpHealthChecker {

    private final McpServerMapper mcpServerMapper;
    private final McpClient mcpClient;

    /**
     * 每 5 分钟检查一次所有 MCP 服务的健康状态
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void checkAllServers() {
        List<McpServer> servers = mcpServerMapper.selectList(null);
        log.info("Starting MCP health check for {} servers", servers.size());

        for (McpServer server : servers) {
            checkServerHealth(server);
        }
    }

    /**
     * 检查单个 MCP 服务器健康状态
     */
    public void checkServerHealth(McpServer server) {
        String previousStatus = server.getHealthStatus();
        try {
            mcpClient.initialize(server.getEndpoint());
            server.setHealthStatus("healthy");
            log.debug("MCP server {} is healthy", server.getName());
        } catch (Exception e) {
            server.setHealthStatus("unhealthy");
            log.warn("MCP server {} is unhealthy: {}", server.getName(), e.getMessage());
        }

        server.setLastCheckAt(LocalDateTime.now());
        mcpServerMapper.updateById(server);

        // 状态变更告警
        if (!server.getHealthStatus().equals(previousStatus)) {
            log.info("MCP server {} status changed: {} -> {}",
                    server.getName(), previousStatus, server.getHealthStatus());
            // 可扩展：推送 WebSocket 告警通知
        }
    }
}
