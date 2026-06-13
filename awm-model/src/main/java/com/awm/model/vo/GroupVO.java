package com.awm.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupVO {
    private String id;
    private String name;
    private String managerId;
    private String managerName;
    private Integer memberCount;
    private LocalDateTime createdAt;
}
