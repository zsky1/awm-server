package com.awm.service.memory;

import com.awm.dal.mapper.AgentMapper;
import com.awm.model.entity.Agent;
import com.awm.model.entity.Memory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 记忆管理器
 * 构建对话上下文 + 检查是否需要摘要长期记忆
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryManager {

    private final MemoryService memoryService;
    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构建对话上下文
     * 1. 获取短期记忆配置（window_size）
     * 2. 取最近 N 轮对话
     * 3. 如果有长期记忆，加载摘要
     */
    @SuppressWarnings("unchecked")
    public String buildContext(String agentId, String sessionId) {
        // 获取短期记忆配置
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            return "";
        }

        int windowSize = 10; // 默认保留 10 轮
        try {
            Map<String, Object> config = objectMapper.readValue(agent.getConfig(), Map.class);
            Map<String, Object> memoryConfig = (Map<String, Object>) config.get("memory");
            if (memoryConfig != null) {
                // 兼容驼峰和蛇形命名（AgentConfigDTO 用驼峰，手动输入可能是蛇形）
                Map<String, Object> shortTerm = (Map<String, Object>) 
                        (memoryConfig.get("shortTerm") != null ? memoryConfig.get("shortTerm") : memoryConfig.get("short_term"));
                if (shortTerm != null && (shortTerm.get("windowSize") != null || shortTerm.get("window_size") != null)) {
                    Object windowSizeObj = shortTerm.get("windowSize") != null ? shortTerm.get("windowSize") : shortTerm.get("window_size");
                    windowSize = ((Number) windowSizeObj).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse agent memory config, using defaults", e);
        }

        // 取最近 N 条短期记忆
        List<Memory> recentMemories = memoryService.getRecentMemories(agentId, windowSize);

        StringBuilder context = new StringBuilder();
        for (Memory memory : recentMemories) {
            context.append(memory.getContent()).append("\n");
        }

        // 加载长期记忆摘要
        List<Memory> longTermMemories = memoryService.getLongTermMemories(agentId);
        if (!longTermMemories.isEmpty()) {
            context.append("\n## 历史经验摘要\n");
            for (Memory memory : longTermMemories) {
                if (memory.getSummary() != null) {
                    context.append(memory.getSummary()).append("\n");
                } else {
                    context.append(memory.getContent()).append("\n");
                }
            }
        }

        return context.toString();
    }

    /**
     * 检查是否需要摘要长期记忆
     * 当短期记忆条数超过 window_size * 2 时，触发摘要
     */
    @SuppressWarnings("unchecked")
    public boolean summarizeIfNeeded(String agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            return false;
        }

        int windowSize = 10;
        try {
            Map<String, Object> config = objectMapper.readValue(agent.getConfig(), Map.class);
            Map<String, Object> memoryConfig = (Map<String, Object>) config.get("memory");
            if (memoryConfig != null) {
                // 兼容驼峰和蛇形命名（AgentConfigDTO 用驼峰，手动输入可能是蛇形）
                Map<String, Object> shortTerm = (Map<String, Object>) 
                        (memoryConfig.get("shortTerm") != null ? memoryConfig.get("shortTerm") : memoryConfig.get("short_term"));
                if (shortTerm != null && (shortTerm.get("windowSize") != null || shortTerm.get("window_size") != null)) {
                    Object windowSizeObj = shortTerm.get("windowSize") != null ? shortTerm.get("windowSize") : shortTerm.get("window_size");
                    windowSize = ((Number) windowSizeObj).intValue();
                }
            }
        } catch (Exception e) {
            // 使用默认值
        }

        List<Memory> recentMemories = memoryService.getRecentMemories(agentId, windowSize * 3);
        return recentMemories.size() > windowSize * 2;
    }
}
