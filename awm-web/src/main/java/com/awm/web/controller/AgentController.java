package com.awm.web.controller;

import com.awm.common.result.PageResult;
import com.awm.common.result.Result;
import com.awm.model.dto.AgentConfigDTO;
import com.awm.model.dto.AgentCreateDTO;
import com.awm.model.dto.AgentUpdateDTO;
import com.awm.model.vo.AgentDetailVO;
import com.awm.model.vo.AgentVO;
import com.awm.service.agent.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 管理接口
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 分页查询 Agent 列表
     */
    @GetMapping
    public Result<PageResult<AgentVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String lifecycleStatus) {
        return Result.success(agentService.listAgents(page, size, keyword, departmentId, lifecycleStatus));
    }

    /**
     * 获取 Agent 详情
     */
    @GetMapping("/{id}")
    public Result<AgentDetailVO> detail(@PathVariable String id) {
        return Result.success(agentService.getAgentById(id));
    }

    /**
     * 创建 Agent
     */
    @PostMapping
    public Result<AgentVO> create(@RequestBody @Valid AgentCreateDTO dto) {
        return Result.success(agentService.createAgent(dto));
    }

    /**
     * 更新 Agent
     */
    @PutMapping("/{id}")
    public Result<AgentVO> update(@PathVariable String id, @RequestBody @Valid AgentUpdateDTO dto) {
        return Result.success(agentService.updateAgent(id, dto));
    }

    /**
     * 删除 Agent（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    /**
     * 更新 Agent 状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        agentService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取 Agent 配置
     */
    @GetMapping("/{id}/config")
    public Result<Map<String, Object>> getConfig(@PathVariable String id) {
        return Result.success(agentService.getConfig(id));
    }

    /**
     * 更新 Agent 配置
     */
    @PutMapping("/{id}/config")
    public Result<Map<String, Object>> updateConfig(@PathVariable String id,
                                                      @RequestBody AgentConfigDTO config) {
        return Result.success(agentService.updateConfig(id, config));
    }
}
