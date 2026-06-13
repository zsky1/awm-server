package com.awm.service.task;

import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.TaskMapper;
import com.awm.model.entity.Agent;
import com.awm.model.entity.Task;
import com.awm.model.vo.TaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final AgentMapper agentMapper;

    /**
     * 查询任务列表
     */
    public List<TaskVO> listTasks(String groupId, String status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(Task::getGroupId, groupId);
        }
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        wrapper.orderByDesc(Task::getCreatedAt);

        return taskMapper.selectList(wrapper).stream()
                .map(this::toTaskVO)
                .toList();
    }

    /**
     * 任务详情
     */
    public TaskVO getTaskById(String id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("Task not found: " + id);
        }
        return toTaskVO(task);
    }

    /**
     * 创建任务
     */
    @Transactional
    public TaskVO createTask(Task task) {
        task.setStatus(task.getStatus() != null ? task.getStatus() : "pending");
        task.setProgress(task.getProgress() != null ? task.getProgress() : 0);
        taskMapper.insert(task);
        return toTaskVO(task);
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public void updateTaskStatus(String id, String status) {
        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, id)
                .set(Task::getStatus, status));
    }

    /**
     * 更新任务进度
     */
    @Transactional
    public void updateTaskProgress(String id, int progress) {
        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, id)
                .set(Task::getProgress, progress));
    }

    /**
     * 获取子任务列表
     */
    public List<TaskVO> getSubTasks(String parentTaskId) {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getParentTaskId, parentTaskId)
                        .orderByAsc(Task::getCreatedAt));
        return tasks.stream().map(this::toTaskVO).toList();
    }

    private TaskVO toTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setGroupId(task.getGroupId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setAssignedAgentId(task.getAssignedAgentId());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setParentTaskId(task.getParentTaskId());
        vo.setPriority(task.getPriority());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        // 查询 Agent 名称
        if (task.getAssignedAgentId() != null) {
            Agent agent = agentMapper.selectById(task.getAssignedAgentId());
            if (agent != null) {
                vo.setAssignedAgentName(agent.getName());
            }
        }

        return vo;
    }
}
