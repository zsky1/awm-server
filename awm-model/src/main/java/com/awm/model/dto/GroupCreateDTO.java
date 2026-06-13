package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class GroupCreateDTO {
    @NotBlank(message = "群组名称不能为空")
    private String name;
    @NotBlank(message = "管理员不能为空")
    private String managerId;
    private List<String> memberIds;
}
