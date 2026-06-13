package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.dto.DeptCreateDTO;
import com.awm.model.vo.DeptTreeVO;
import com.awm.service.agent.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理接口
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    public Result<List<DeptTreeVO>> getDeptTree() {
        return Result.success(departmentService.getDeptTree());
    }

    /**
     * 创建部门
     */
    @PostMapping
    public Result<DeptTreeVO> create(@RequestBody @Valid DeptCreateDTO dto) {
        return Result.success(departmentService.createDept(dto));
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    public Result<DeptTreeVO> update(@PathVariable String id, @RequestBody @Valid DeptCreateDTO dto) {
        return Result.success(departmentService.updateDept(id, dto));
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        departmentService.deleteDept(id);
        return Result.success();
    }
}
