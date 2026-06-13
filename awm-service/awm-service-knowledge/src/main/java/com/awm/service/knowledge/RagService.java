package com.awm.service.knowledge;

import com.awm.ai.client.EmbeddingClient;
import com.awm.dal.mapper.AgentKnowledgeBindingMapper;
import com.awm.dal.mapper.KnowledgeBaseMapper;
import com.awm.model.entity.AgentKnowledgeBinding;
import com.awm.model.entity.KnowledgeBase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 检索服务
 * 1. 查询 Agent 绑定的知识库
 * 2. 向量化 query
 * 3. 在 Qdrant 中检索
 * 4. 返回相关文档片段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingClient embeddingClient;

    /**
     * RAG 检索
     *
     * @param agentId Agent ID
     * @param query   查询文本
     * @param topK    返回最相关的 K 个结果
     * @return 相关文档片段列表
     */
    public List<Map<String, Object>> retrieve(String agentId, String query, int topK) {
        // 1. 查询 Agent 绑定的知识库
        List<AgentKnowledgeBinding> bindings = agentKnowledgeBindingMapper.selectList(
                new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getAgentId, agentId));

        if (bindings.isEmpty()) {
            return List.of();
        }

        // 2. 向量化 query
        float[] queryVector;
        try {
            queryVector = embeddingClient.embed(query);
        } catch (Exception e) {
            log.error("Failed to embed query: {}", query, e);
            return List.of();
        }

        // 3. 在 Qdrant 中检索每个绑定的知识库
        List<Map<String, Object>> allResults = new ArrayList<>();
        for (AgentKnowledgeBinding binding : bindings) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(binding.getKbId());
            if (kb == null) continue;

            try {
                // TODO: 调用 Qdrant API 进行向量检索
                // List<QdrantSearchResult> results = qdrantClient.search(
                //     kb.getVectorCollection(), queryVector, topK);
                //
                // for (QdrantSearchResult result : results) {
                //     Map<String, Object> item = new HashMap<>();
                //     item.put("content", result.getPayload().get("content"));
                //     item.put("score", result.getScore());
                //     item.put("kbId", kb.getId());
                //     item.put("kbName", kb.getName());
                //     allResults.add(item);
                // }
            } catch (Exception e) {
                log.warn("Failed to search in knowledge base: {}", kb.getName(), e);
            }
        }

        // 4. 按 score 排序，取 topK
        allResults.sort((a, b) -> {
            double scoreA = ((Number) a.getOrDefault("score", 0)).doubleValue();
            double scoreB = ((Number) b.getOrDefault("score", 0)).doubleValue();
            return Double.compare(scoreB, scoreA);
        });

        return allResults.stream().limit(topK).toList();
    }
}
