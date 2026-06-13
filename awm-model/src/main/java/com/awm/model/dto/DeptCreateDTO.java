package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptCreateDTO {
    @NotBlank(message = "部门名称不能为空")
    private String name;
    private String parentId;
    private Integer sortOrder;
}
