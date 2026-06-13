package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.dto.McpServerCreateDTO;
import com.awm.model.vo.McpServerVO;
import com.awm.service.mcp.McpServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务管理接口
 */
@RestController
@RequestMapping("/api/mcp/servers")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    /**
     * 查询 MCP 服务列表
     */
    @GetMapping
    public Result<List<McpServerVO>> list() {
        return Result.success(mcpServerService.listServers());
    }

    /**
     * 注册 MCP 服务
     */
    @PostMapping
    public Result<McpServerVO> create(@RequestBody @Valid McpServerCreateDTO dto) {
        return Result.success(mcpServerService.createServer(dto));
    }

    /**
     * 更新 MCP 服务
     */
    @PutMapping("/{id}")
    public Result<McpServerVO> update(@PathVariable String id, @RequestBody @Valid McpServerCreateDTO dto) {
        return Result.success(mcpServerService.updateServer(id, dto));
    }

    /**
     * 删除 MCP 服务
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        mcpServerService.deleteServer(id);
        return Result.success();
    }

    /**
     * 测试连通性
     */
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(@PathVariable String id) {
        return Result.success(mcpServerService.testConnection(id));
    }

    /**
     * 发现工具
     */
    @GetMapping("/{id}/tools")
    public Result<List<Map<String, Object>>> discoverTools(@PathVariable String id) {
        return Result.success(mcpServerService.discoverTools(id));
    }

    /**
     * 绑定到 Agent
     */
    @PostMapping("/{id}/bind/{agentId}")
    public Result<Void> bindToAgent(@PathVariable String id, @PathVariable String agentId) {
        mcpServerService.bindToAgent(agentId, id);
        return Result.success();
    }

    /**
     * 解绑
     */
    @DeleteMapping("/{id}/bind/{agentId}")
    public Result<Void> unbindFromAgent(@PathVariable String id, @PathVariable String agentId) {
        mcpServerService.unbindFromAgent(agentId, id);
        return Result.success();
    }
}
