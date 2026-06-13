package com.awm.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptTreeVO {
    private String id;
    private String name;
    private String parentId;
    private Integer sortOrder;
    private Integer agentCount;
    private List<DeptTreeVO> children;
}
