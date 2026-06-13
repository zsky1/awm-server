package com.awm.service.mcp;

import com.awm.ai.client.McpClient;
import com.awm.dal.mapper.AgentMcpBindingMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.model.dto.McpServerCreateDTO;
import com.awm.model.entity.AgentMcpBinding;
import com.awm.model.entity.McpServer;
import com.awm.model.vo.McpServerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerMapper mcpServerMapper;
    private final AgentMcpBindingMapper agentMcpBindingMapper;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    /**
     * 查询 MCP 服务列表
     */
    public List<McpServerVO> listServers() {
        List<McpServer> servers = mcpServerMapper.selectList(
                new LambdaQueryWrapper<McpServer>().orderByDesc(McpServer::getCreatedAt));
        return servers.stream().map(this::toMcpServerVO).toList();
    }

    /**
     * 注册 MCP 服务
     */
    @Transactional
    public McpServerVO createServer(McpServerCreateDTO dto) {
        McpServer server = new McpServer();
        server.setName(dto.getName());
        server.setEndpoint(dto.getEndpoint());
        server.setDescription(dto.getDescription());
        server.setHealthStatus("unknown");
        server.setTools("[]");
        server.setConfig("{}");

        mcpServerMapper.insert(server);
        return toMcpServerVO(server);
    }

    /**
     * 更新 MCP 服务
     */
    @Transactional
    public McpServerVO updateServer(String id, McpServerCreateDTO dto) {
        McpServer server = mcpServerMapper.selectById(id);
        if (server == null) {
            throw new RuntimeException("MCP Server not found: " + id);
        }
        if (dto.getName() != null) server.setName(dto.getName());
        if (dto.getEndpoint() != null) server.setEndpoint(dto.getEndpoint());
        if (dto.getDescription() != null) server.setDescription(dto.getDescription());

        mcpServerMapper.updateById(server);
        return toMcpServerVO(server);
    }

    /**
     * 删除 MCP 服务
     */
    @Transactional
    public void deleteServer(String id) {
        // 删除关联的绑定
        agentMcpBindingMapper.delete(
                new LambdaQueryWrapper<AgentMcpBinding>().eq(AgentMcpBinding::getMcpServerId, id));
        mcpServerMapper.deleteById(id);
    }

    /**
     * 测试连通性（调用 McpClient.initialize()）
     */
    public Map<String, Object> testConnection(String id) {
        McpServer server = mcpServerMapper.selectById(id);
        if (server == null) {
            throw new RuntimeException("MCP Server not found: " + id);
        }

        try {
            long startTime = System.currentTimeMillis();
            var result = mcpClient.initialize(server.getEndpoint());
            long latency = System.currentTimeMillis() - startTime;

            // 更新健康状态
            server.setHealthStatus("healthy");
            server.setLastCheckAt(java.time.LocalDateTime.now());
            mcpServerMapper.updateById(server);

            return Map.of(
                    "status", "healthy",
                    "latencyMs", latency,
                    "serverInfo", result
            );
        } catch (Exception e) {
            server.setHealthStatus("unhealthy");
            server.setLastCheckAt(java.time.LocalDateTime.now());
            mcpServerMapper.updateById(server);

            return Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 发现 MCP 工具（调用 McpClient.listTools()）
     */
    public List<Map<String, Object>> discoverTools(String id) {
        McpServer server = mcpServerMapper.selectById(id);
        if (server == null) {
            throw new RuntimeException("MCP Server not found: " + id);
        }

        List<Map<String, Object>> tools = mcpClient.listTools(server.getEndpoint());

        // 保存工具列表
        try {
            server.setTools(objectMapper.writeValueAsString(tools));
            mcpServerMapper.updateById(server);
        } catch (Exception e) {
            log.warn("Failed to save tools list", e);
        }

        return tools;
    }

    /**
     * 绑定到 Agent
     */
    @Transactional
    public void bindToAgent(String agentId, String mcpServerId) {
        // 检查是否已绑定
        Long count = agentMcpBindingMapper.selectCount(
                new LambdaQueryWrapper<AgentMcpBinding>()
                        .eq(AgentMcpBinding::getAgentId, agentId)
                        .eq(AgentMcpBinding::getMcpServerId, mcpServerId));

        if (count > 0) {
            // 已绑定，更新 enabled
            agentMcpBindingMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AgentMcpBinding>()
                            .eq(AgentMcpBinding::getAgentId, agentId)
                            .eq(AgentMcpBinding::getMcpServerId, mcpServerId)
                            .set(AgentMcpBinding::getEnabled, true));
            return;
        }

        AgentMcpBinding binding = new AgentMcpBinding();
        binding.setAgentId(agentId);
        binding.setMcpServerId(mcpServerId);
        binding.setEnabled(true);
        binding.setConfig("{}");
        agentMcpBindingMapper.insert(binding);
    }

    /**
     * 解绑
     */
    @Transactional
    public void unbindFromAgent(String agentId, String mcpServerId) {
        agentMcpBindingMapper.delete(
                new LambdaQueryWrapper<AgentMcpBinding>()
                        .eq(AgentMcpBinding::getAgentId, agentId)
                        .eq(AgentMcpBinding::getMcpServerId, mcpServerId));
    }

    private McpServerVO toMcpServerVO(McpServer server) {
        McpServerVO vo = new McpServerVO();
        vo.setId(server.getId());
        vo.setName(server.getName());
        vo.setEndpoint(server.getEndpoint());
        vo.setDescription(server.getDescription());
        vo.setTools(server.getTools());
        vo.setHealthStatus(server.getHealthStatus());
        vo.setLastCheckAt(server.getLastCheckAt());
        return vo;
    }
}
