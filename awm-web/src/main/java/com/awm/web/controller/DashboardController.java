package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.dal.mapper.TaskMapper;
import com.awm.model.entity.Agent;
import com.awm.model.entity.McpServer;
import com.awm.model.entity.Task;
import com.awm.model.vo.DashboardVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘接口
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AgentMapper agentMapper;
    private final McpServerMapper mcpServerMapper;
    private final TaskMapper taskMapper;

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<DashboardVO> getStats() {
        DashboardVO vo = new DashboardVO();

        // Agent 统计
        Long totalAgents = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().ne(Agent::getLifecycleStatus, "archived"));
        Long onlineAgents = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getRuntimeStatus, "idle"));
        Long busyAgents = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getRuntimeStatus, "busy"));
        Long errorAgents = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getRuntimeStatus, "error"));
        Long offlineAgents = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getRuntimeStatus, "offline"));

        vo.setTotalAgents(totalAgents.intValue());
        vo.setOnlineAgents(onlineAgents.intValue());
        vo.setBusyAgents(busyAgents.intValue());
        vo.setErrorAgents(errorAgents.intValue());
        vo.setOfflineAgents(offlineAgents.intValue());

        // 任务统计
        Long pendingTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "pending"));
        Long inProgressTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "in_progress"));
        Long completedTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "completed"));
        Long failedTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "failed"));

        vo.setPendingTasks(pendingTasks.intValue());
        vo.setInProgressTasks(inProgressTasks.intValue());
        vo.setCompletedTasks(completedTasks.intValue());
        vo.setFailedTasks(failedTasks.intValue());

        // MCP 服务统计
        Long totalMcpServers = mcpServerMapper.selectCount(null);
        Long healthyServers = mcpServerMapper.selectCount(
                new LambdaQueryWrapper<McpServer>().eq(McpServer::getHealthStatus, "healthy"));
        Long unhealthyServers = mcpServerMapper.selectCount(
                new LambdaQueryWrapper<McpServer>().eq(McpServer::getHealthStatus, "unhealthy"));

        vo.setTotalMcpServers(totalMcpServers.intValue());
        vo.setHealthyMcpServers(healthyServers.intValue());
        vo.setUnhealthyMcpServers(unhealthyServers.intValue());

        return Result.success(vo);
    }
}
