package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.vo.TaskVO;
import com.awm.service.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理接口
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 查询任务列表
     */
    @GetMapping
    public Result<List<TaskVO>> list(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String status) {
        return Result.success(taskService.listTasks(groupId, status));
    }

    /**
     * 任务详情
     */
    @GetMapping("/{id}")
    public Result<TaskVO> detail(@PathVariable String id) {
        return Result.success(taskService.getTaskById(id));
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        taskService.updateTaskStatus(id, status);
        return Result.success();
    }

    /**
     * 更新任务进度
     */
    @PutMapping("/{id}/progress")
    public Result<Void> updateProgress(@PathVariable String id, @RequestParam int progress) {
        taskService.updateTaskProgress(id, progress);
        return Result.success();
    }
}
