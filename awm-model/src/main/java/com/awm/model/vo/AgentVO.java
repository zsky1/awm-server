package com.awm.model.vo;

import com.awm.model.dto.AgentConfigDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentVO {
    private String id;
    private String name;
    private String avatar;
    private String position;
    private String departmentId;
    private String lifecycleStatus;
    private String runtimeStatus;
    private String modelConfigId;
    private List<AgentConfigDTO.SkillItem> skills;
    private LocalDateTime createdAt;
}
