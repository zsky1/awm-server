package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.entity.KnowledgeBase;
import com.awm.service.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理接口
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库列表
     */
    @GetMapping
    public Result<List<KnowledgeBase>> list() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public Result<KnowledgeBase> create(@RequestParam String name,
                                         @RequestParam(required = false) String description) {
        return Result.success(knowledgeBaseService.createKnowledgeBase(name, description));
    }

    /**
     * 上传文档
     */
    @PostMapping("/{id}/documents")
    public Result<Void> uploadDocument(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        knowledgeBaseService.uploadDocument(id, file);
        return Result.success();
    }

    /**
     * 重新索引
     */
    @PostMapping("/{id}/reindex")
    public Result<Void> reindex(@PathVariable String id) {
        knowledgeBaseService.reindex(id);
        return Result.success();
    }

    /**
     * 获取索引状态
     */
    @GetMapping("/{id}/status")
    public Result<String> getIndexStatus(@PathVariable String id) {
        return Result.success(knowledgeBaseService.getIndexStatus(id));
    }
}
