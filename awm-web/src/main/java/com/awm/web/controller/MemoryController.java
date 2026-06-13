package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.entity.Memory;
import com.awm.service.memory.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆管理接口
 */
@RestController
@RequestMapping("/api/agents/{agentId}/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * 获取 Agent 记忆
     */
    @GetMapping
    public Result<List<Memory>> getMemories(
            @PathVariable String agentId,
            @RequestParam(required = false) String type) {
        return Result.success(memoryService.getMemories(agentId, type));
    }

    /**
     * 清空 Agent 记忆
     */
    @DeleteMapping
    public Result<Void> clearMemories(
            @PathVariable String agentId,
            @RequestParam(required = false) String type) {
        memoryService.clearMemories(agentId, type);
        return Result.success();
    }
}
