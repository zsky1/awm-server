package com.awm.service.agent;

import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.DepartmentMapper;
import com.awm.model.dto.DeptCreateDTO;
import com.awm.model.entity.Agent;
import com.awm.model.entity.Department;
import com.awm.model.vo.DeptTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final AgentMapper agentMapper;

    /**
     * 递归构建部门树，每个节点附带 agentCount
     */
    public List<DeptTreeVO> getDeptTree() {
        List<Department> allDepts = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getSortOrder));
        List<Agent> allAgents = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>().eq(Agent::getLifecycleStatus, "active"));

        // 统计每个部门的 Agent 数量
        Map<String, Long> agentCountMap = allAgents.stream()
                .filter(a -> a.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Agent::getDepartmentId, Collectors.counting()));

        // 构建树
        List<DeptTreeVO> roots = allDepts.stream()
                .filter(d -> d.getParentId() == null)
                .map(d -> toDeptTreeVO(d, allDepts, agentCountMap))
                .toList();

        return roots;
    }

    /**
     * 创建部门
     */
    @Transactional
    public DeptTreeVO createDept(DeptCreateDTO dto) {
        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setParentId(dto.getParentId());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        departmentMapper.insert(dept);
        return toDeptTreeVO(dept, List.of(), Map.of());
    }

    /**
     * 更新部门
     */
    @Transactional
    public DeptTreeVO updateDept(String id, DeptCreateDTO dto) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new RuntimeException("Department not found: " + id);
        }
        if (dto.getName() != null) dept.setName(dto.getName());
        if (dto.getParentId() != null) dept.setParentId(dto.getParentId());
        if (dto.getSortOrder() != null) dept.setSortOrder(dto.getSortOrder());

        departmentMapper.updateById(dept);
        return toDeptTreeVO(dept, List.of(), Map.of());
    }

    /**
     * 删除部门（检查是否有子部门和 Agent）
     */
    @Transactional
    public void deleteDept(String id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new RuntimeException("Department not found: " + id);
        }

        // 检查子部门
        Long childCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("该部门下存在子部门，无法删除");
        }

        // 检查 Agent
        Long agentCount = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getDepartmentId, id)
                        .ne(Agent::getLifecycleStatus, "archived"));
        if (agentCount > 0) {
            throw new RuntimeException("该部门下存在 Agent，无法删除");
        }

        departmentMapper.deleteById(id);
    }

    private DeptTreeVO toDeptTreeVO(Department dept, List<Department> allDepts,
                                     Map<String, Long> agentCountMap) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setSortOrder(dept.getSortOrder());
        vo.setAgentCount(agentCountMap.getOrDefault(dept.getId(), 0L).intValue());

        List<DeptTreeVO> children = allDepts.stream()
                .filter(d -> dept.getId().equals(d.getParentId()))
                .map(d -> toDeptTreeVO(d, allDepts, agentCountMap))
                .toList();
        vo.setChildren(children);

        return vo;
    }
}
