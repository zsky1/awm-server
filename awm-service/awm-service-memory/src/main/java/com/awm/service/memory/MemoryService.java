package com.awm.service.memory;

import com.awm.dal.mapper.MemoryMapper;
import com.awm.model.entity.Memory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 记忆管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryMapper memoryMapper;

    /**
     * 获取 Agent 记忆
     */
    public List<Memory> getMemories(String agentId, String type) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getAgentId, agentId);
        if (type != null) {
            wrapper.eq(Memory::getType, type);
        }
        wrapper.orderByDesc(Memory::getCreatedAt);
        return memoryMapper.selectList(wrapper);
    }

    /**
     * 添加记忆
     */
    @Transactional
    public Memory addMemory(String agentId, String type, String content, String sessionId) {
        Memory memory = new Memory();
        memory.setAgentId(agentId);
        memory.setType(type);
        memory.setContent(content);
        memory.setSessionId(sessionId);

        memoryMapper.insert(memory);
        return memory;
    }

    /**
     * 清空记忆
     */
    @Transactional
    public void clearMemories(String agentId, String type) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getAgentId, agentId);
        if (type != null) {
            wrapper.eq(Memory::getType, type);
        }
        memoryMapper.delete(wrapper);
    }

    /**
     * 获取最近 N 条短期记忆
     */
    public List<Memory> getRecentMemories(String agentId, int limit) {
        return memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getAgentId, agentId)
                        .eq(Memory::getType, "short_term")
                        .orderByDesc(Memory::getCreatedAt)
                        .last("LIMIT " + limit));
    }

    /**
     * 获取长期记忆
     */
    public List<Memory> getLongTermMemories(String agentId) {
        return memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getAgentId, agentId)
                        .eq(Memory::getType, "long_term")
                        .orderByDesc(Memory::getCreatedAt));
    }
}
