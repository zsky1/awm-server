package com.awm.model.entity;

import com.awm.common.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "knowledge_base", autoResultMap = true)
public class KnowledgeBase {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String description;
    private String vectorCollection;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String indexStatus;
    private LocalDateTime lastIndexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
