package com.awm.service.knowledge;

import com.awm.ai.client.EmbeddingClient;
import com.awm.dal.mapper.AgentKnowledgeBindingMapper;
import com.awm.dal.mapper.KnowledgeBaseMapper;
import com.awm.model.entity.AgentKnowledgeBinding;
import com.awm.model.entity.KnowledgeBase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    @Value("${awm.storage.path:/tmp/awm/documents}")
    private String storagePath;

    /**
     * 查询知识库列表
     */
    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>().orderByDesc(KnowledgeBase::getCreatedAt));
    }

    /**
     * 创建知识库（创建 Qdrant collection）
     */
    @Transactional
    public KnowledgeBase createKnowledgeBase(String name, String description) {
        String collectionName = "kb_" + com.awm.common.util.UuidUtils.randomCompact().substring(0, 12);

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setVectorCollection(collectionName);
        kb.setIndexStatus("{\"completed\":0,\"total\":0}");
        kb.setLastIndexedAt(null);

        // TODO: 调用 Qdrant API 创建 collection
        // qdrantClient.createCollection(collectionName, embeddingDimensions);

        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("Knowledge base not found: " + id);
        }

        // 删除关联绑定
        agentKnowledgeBindingMapper.delete(
                new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getKbId, id));

        // TODO: 删除 Qdrant collection
        // qdrantClient.deleteCollection(kb.getVectorCollection());

        knowledgeBaseMapper.deleteById(id);
    }

    /**
     * 上传文档
     * 1. 保存文件到 MinIO/本地
     * 2. 文档切片
     * 3. 调用 Embedding API 向量化
     * 4. 存入 Qdrant
     * 5. 更新索引状态
     */
    @Transactional
    public void uploadDocument(String kbId, MultipartFile file) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("Knowledge base not found: " + kbId);
        }

        try {
            // 1. 保存文件
            String filename = com.awm.common.util.UuidUtils.random() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(storagePath, kbId);
            Files.createDirectories(filePath);
            Path fullPath = filePath.resolve(filename);
            file.transferTo(fullPath.toFile());

            // 2. 文档切片（简化实现，按段落切片）
            String content = new String(file.getBytes());
            List<String> chunks = splitDocument(content, 500, 50);

            // 3 & 4. 向量化并存入 Qdrant
            int completed = 0;
            for (String chunk : chunks) {
                try {
                    float[] vector = embeddingClient.embed(chunk);
                    // TODO: 存入 Qdrant
                    // qdrantClient.upsert(kb.getVectorCollection(), vector, chunk);
                    completed++;
                } catch (Exception e) {
                    log.warn("Failed to embed chunk: {}", chunk.substring(0, Math.min(50, chunk.length())), e);
                }
            }

            // 5. 更新索引状态
            kb.setIndexStatus(String.format("{\"completed\":%d,\"total\":%d}", completed, chunks.size()));
            kb.setLastIndexedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(kb);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    /**
     * 重新索引
     */
    @Transactional
    public void reindex(String kbId) {
        // 重置索引状态，重新处理所有文档
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("Knowledge base not found: " + kbId);
        }
        kb.setIndexStatus("{\"completed\":0,\"total\":0}");
        knowledgeBaseMapper.updateById(kb);

        // TODO: 扫描 storagePath 下的所有文档，重新切片+向量化
        log.info("Reindex triggered for knowledge base: {}", kbId);
    }

    /**
     * 获取索引状态
     */
    public String getIndexStatus(String kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("Knowledge base not found: " + kbId);
        }
        return kb.getIndexStatus();
    }

    /**
     * 绑定到 Agent
     */
    @Transactional
    public void bindToAgent(String agentId, String kbId, String alias) {
        Long count = agentKnowledgeBindingMapper.selectCount(
                new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getAgentId, agentId)
                        .eq(AgentKnowledgeBinding::getKbId, kbId));

        if (count > 0) {
            return; // 已绑定
        }

        AgentKnowledgeBinding binding = new AgentKnowledgeBinding();
        binding.setAgentId(agentId);
        binding.setKbId(kbId);
        binding.setAlias(alias);
        binding.setSyncInterval(3600);
        agentKnowledgeBindingMapper.insert(binding);
    }

    /**
     * 解绑
     */
    @Transactional
    public void unbindFromAgent(String agentId, String kbId) {
        agentKnowledgeBindingMapper.delete(
                new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getAgentId, agentId)
                        .eq(AgentKnowledgeBinding::getKbId, kbId));
    }

    /**
     * 文档切片（按字符数切片，带重叠）
     */
    private List<String> splitDocument(String content, int chunkSize, int overlap) {
        List<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
