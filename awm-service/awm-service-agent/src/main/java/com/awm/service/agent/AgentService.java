package com.awm.service.agent;

import com.awm.common.result.PageResult;
import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.AgentMcpBindingMapper;
import com.awm.dal.mapper.AgentKnowledgeBindingMapper;
import com.awm.dal.mapper.DepartmentMapper;
import com.awm.dal.mapper.McpServerMapper;
import com.awm.dal.mapper.ModelConfigMapper;
import com.awm.model.dto.AgentConfigDTO;
import com.awm.model.dto.AgentCreateDTO;
import com.awm.model.dto.AgentUpdateDTO;
import com.awm.model.entity.Agent;
import com.awm.model.entity.AgentMcpBinding;
import com.awm.model.entity.AgentKnowledgeBinding;
import com.awm.model.entity.Department;
import com.awm.model.entity.McpServer;
import com.awm.model.entity.ModelConfig;
import com.awm.model.vo.AgentDetailVO;
import com.awm.model.vo.AgentVO;
import com.awm.model.vo.KnowledgeBaseVO;
import com.awm.model.vo.McpServerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Agent 管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final AgentMcpBindingMapper agentMcpBindingMapper;
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;
    private final McpServerMapper mcpServerMapper;
    private final DepartmentMapper departmentMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 Agent
     */
    public PageResult<AgentVO> listAgents(int page, int size, String keyword,
                                           String departmentId, String lifecycleStatus) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Agent::getName, keyword)
                    .or().like(Agent::getPosition, keyword));
        }
        if (departmentId != null) {
            wrapper.eq(Agent::getDepartmentId, departmentId);
        }
        if (StringUtils.hasText(lifecycleStatus)) {
            wrapper.eq(Agent::getLifecycleStatus, lifecycleStatus);
        }
        wrapper.orderByDesc(Agent::getCreatedAt);

        IPage<Agent> agentPage = agentMapper.selectPage(new Page<>(page, size), wrapper);

        List<AgentVO> voList = agentPage.getRecords().stream()
                .map(this::toAgentVO)
                .toList();

        return PageResult.of(voList, agentPage.getTotal(), page, size);
    }

    /**
     * 获取 Agent 详情
     */
    public AgentDetailVO getAgentById(String id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }
        return toAgentDetailVO(agent);
    }

    /**
     * 创建 Agent
     */
    @Transactional
    public AgentVO createAgent(AgentCreateDTO dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setAvatar(dto.getAvatar());
        agent.setPosition(dto.getPosition());
        agent.setDepartmentId(dto.getDepartmentId());
        agent.setSupervisorId(dto.getSupervisorId());
        agent.setPersonaPrompt(dto.getPersonaPrompt());
        agent.setLifecycleStatus("active");
        agent.setRuntimeStatus("offline");
        agent.setModelConfigId(dto.getModelConfigId());
        agent.setConfig("{}");

        agentMapper.insert(agent);
        return toAgentVO(agent);
    }

    /**
     * 更新 Agent 基础信息
     */
    @Transactional
    public AgentVO updateAgent(String id, AgentUpdateDTO dto) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }

        if (dto.getName() != null) agent.setName(dto.getName());
        if (dto.getAvatar() != null) agent.setAvatar(dto.getAvatar());
        if (dto.getPosition() != null) agent.setPosition(dto.getPosition());
        if (dto.getDepartmentId() != null) agent.setDepartmentId(dto.getDepartmentId());
        if (dto.getSupervisorId() != null) agent.setSupervisorId(dto.getSupervisorId());
        if (dto.getPersonaPrompt() != null) agent.setPersonaPrompt(dto.getPersonaPrompt());
        if (dto.getModelConfigId() != null) agent.setModelConfigId(dto.getModelConfigId());

        agentMapper.updateById(agent);
        return toAgentVO(agent);
    }

    /**
     * 软删除 Agent（设置 lifecycleStatus=archived）
     */
    @Transactional
    public void deleteAgent(String id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }
        agent.setLifecycleStatus("archived");
        agentMapper.updateById(agent);
    }

    /**
     * 更新 Agent 运行时状态
     */
    @Transactional
    public void updateStatus(String id, String status) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }
        agent.setRuntimeStatus(status);
        agentMapper.updateById(agent);
    }

    /**
     * 获取 Agent 配置（从 config JSONB 解析）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfig(String id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }
        if (agent.getConfig() == null || agent.getConfig().isBlank()) {
            return java.util.Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(agent.getConfig(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse agent config", e);
        }
    }

    /**
     * 更新 Agent 配置（序列化为 JSONB）
     */
    @Transactional
    public Map<String, Object> updateConfig(String id, AgentConfigDTO configDTO) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + id);
        }
        try {
            String configJson = objectMapper.writeValueAsString(configDTO);
            agent.setConfig(configJson);
            agentMapper.updateById(agent);
            return objectMapper.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize agent config", e);
        }
    }

    /**
     * 根据技能标签搜索 Agent（JSONB 查询）
     */
    public List<AgentVO> searchBySkill(String skillTag) {
        // 使用参数化 JSONB 查询，避免 SQL 注入
        String jsonValue;
        try {
            jsonValue = objectMapper.writeValueAsString(List.of(Map.of("tag", skillTag)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize skill tag", e);
        }
        List<Agent> agents = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                        .apply("config->'skills' @> {0}::jsonb", jsonValue)
                        .eq(Agent::getLifecycleStatus, "active")
        );
        return agents.stream().map(this::toAgentVO).toList();
    }

    @SuppressWarnings("unchecked")
    private AgentVO toAgentVO(Agent agent) {
        AgentVO vo = new AgentVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setAvatar(agent.getAvatar());
        vo.setPosition(agent.getPosition());
        vo.setDepartmentId(agent.getDepartmentId());
        vo.setLifecycleStatus(agent.getLifecycleStatus());
        vo.setRuntimeStatus(agent.getRuntimeStatus());
        vo.setModelConfigId(agent.getModelConfigId());
        vo.setCreatedAt(agent.getCreatedAt());

        // 从 config JSONB 解析 skills
        if (agent.getConfig() != null && !agent.getConfig().isBlank()) {
            try {
                Map<String, Object> config = objectMapper.readValue(agent.getConfig(), Map.class);
                Object skillsObj = config.get("skills");
                if (skillsObj instanceof List<?> skillsList) {
                    List<AgentConfigDTO.SkillItem> skills = new java.util.ArrayList<>();
                    for (Object item : skillsList) {
                        if (item instanceof Map<?, ?> map) {
                            AgentConfigDTO.SkillItem si = new AgentConfigDTO.SkillItem();
                            si.setTag((String) map.get("tag"));
                            si.setDescription((String) map.get("description"));
                            si.setLevel((String) map.get("level"));
                            si.setBoundTool((String) map.get("boundTool"));
                            skills.add(si);
                        }
                    }
                    vo.setSkills(skills);
                }
            } catch (Exception e) {
                log.debug("Failed to parse skills from agent config", e);
            }
        }

        return vo;
    }

    @SuppressWarnings("unchecked")
    private AgentDetailVO toAgentDetailVO(Agent agent) {
        AgentDetailVO vo = new AgentDetailVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setAvatar(agent.getAvatar());
        vo.setPosition(agent.getPosition());
        vo.setDepartmentId(agent.getDepartmentId());
        vo.setPersonaPrompt(agent.getPersonaPrompt());
        vo.setLifecycleStatus(agent.getLifecycleStatus());
        vo.setRuntimeStatus(agent.getRuntimeStatus());
        vo.setModelConfigId(agent.getModelConfigId());
        vo.setCreatedAt(agent.getCreatedAt());

        // 查询模型配置名称
        if (agent.getModelConfigId() != null) {
            ModelConfig modelConfig = modelConfigMapper.selectById(agent.getModelConfigId());
            if (modelConfig != null) {
                vo.setModelName(modelConfig.getName());
            }
        }

        // 从 config JSONB 解析关联数据
        if (agent.getConfig() != null && !agent.getConfig().isBlank()) {
            try {
                Map<String, Object> config = objectMapper.readValue(agent.getConfig(), Map.class);

                // 解析 skills（AgentDetailVO 继承自 AgentVO，需显式设置）
                Object skillsObj = config.get("skills");
                if (skillsObj instanceof List<?> skillsList) {
                    List<AgentConfigDTO.SkillItem> skills = new java.util.ArrayList<>();
                    for (Object item : skillsList) {
                        if (item instanceof Map<?, ?> map) {
                            AgentConfigDTO.SkillItem si = new AgentConfigDTO.SkillItem();
                            si.setTag((String) map.get("tag"));
                            si.setDescription((String) map.get("description"));
                            si.setLevel((String) map.get("level"));
                            si.setBoundTool((String) map.get("boundTool"));
                            skills.add(si);
                        }
                    }
                    vo.setSkills(skills);
                }

                // 解析 rules
                Object rulesObj = config.get("rules");
                if (rulesObj instanceof List<?> rulesList) {
                    List<AgentConfigDTO.RuleItem> rules = new java.util.ArrayList<>();
                    for (Object item : rulesList) {
                        if (item instanceof Map<?, ?> map) {
                            AgentConfigDTO.RuleItem ri = new AgentConfigDTO.RuleItem();
                            ri.setType((String) map.get("type"));
                            ri.setContent((String) map.get("content"));
                            if (map.get("priority") instanceof Number n) {
                                ri.setPriority(n.intValue());
                            }
                            rules.add(ri);
                        }
                    }
                    vo.setRules(rules);
                }

                // 解析 memoryConfig（兼容驼峰和蛇形命名）
                Object memoryObj = config.get("memory");
                if (memoryObj instanceof Map<?, ?> memoryMap) {
                    AgentConfigDTO.MemoryConfig mc = new AgentConfigDTO.MemoryConfig();
                    Object shortObj = memoryMap.get("shortTerm") != null ? memoryMap.get("shortTerm") : memoryMap.get("short_term");
                    if (shortObj instanceof Map<?, ?> shortMap) {
                        AgentConfigDTO.ShortTermConfig st = new AgentConfigDTO.ShortTermConfig();
                        Object maxMsgObj = shortMap.get("maxMessages") != null ? shortMap.get("maxMessages") : shortMap.get("max_messages");
                        Object maxTokObj = shortMap.get("maxTokens") != null ? shortMap.get("maxTokens") : shortMap.get("max_tokens");
                        if (maxMsgObj instanceof Number n) st.setMaxMessages(n.intValue());
                        if (maxTokObj instanceof Number n) st.setMaxTokens(n.intValue());
                        mc.setShortTerm(st);
                    }
                    Object longObj = memoryMap.get("longTerm") != null ? memoryMap.get("longTerm") : memoryMap.get("long_term");
                    if (longObj instanceof Map<?, ?> longMap) {
                        AgentConfigDTO.LongTermConfig lt = new AgentConfigDTO.LongTermConfig();
                        if (longMap.get("enabled") instanceof Boolean b) lt.setEnabled(b);
                        Object intervalObj = longMap.get("summaryInterval") != null ? longMap.get("summaryInterval") : longMap.get("summary_interval");
                        if (intervalObj instanceof String s) lt.setSummaryInterval(s);
                        mc.setLongTerm(lt);
                    }
                    vo.setMemoryConfig(mc);
                }

            } catch (Exception e) {
                log.debug("Failed to parse agent config for detail VO", e);
            }
        }

        // 查询部门名称
        if (agent.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(agent.getDepartmentId());
            if (dept != null) {
                vo.setDepartmentName(dept.getName());
            }
        }

        // 查询上级名称
        if (agent.getSupervisorId() != null) {
            Agent supervisor = agentMapper.selectById(agent.getSupervisorId());
            if (supervisor != null) {
                vo.setSupervisorName(supervisor.getName());
            }
        }

        // 查询 MCP 绑定并填充 McpServerVO
        List<AgentMcpBinding> mcpBindings = agentMcpBindingMapper.selectList(
                new LambdaQueryWrapper<AgentMcpBinding>().eq(AgentMcpBinding::getAgentId, agent.getId()));
        if (!mcpBindings.isEmpty()) {
            List<McpServerVO> mcpVOs = new java.util.ArrayList<>();
            for (AgentMcpBinding binding : mcpBindings) {
                McpServer server = mcpServerMapper.selectById(binding.getMcpServerId());
                if (server != null) {
                    McpServerVO mcpVO = new McpServerVO();
                    mcpVO.setId(server.getId());
                    mcpVO.setName(server.getName());
                    mcpVO.setEndpoint(server.getEndpoint());
                    mcpVO.setDescription(server.getDescription());
                    mcpVO.setTools(server.getTools());
                    mcpVO.setHealthStatus(server.getHealthStatus());
                    mcpVO.setLastCheckAt(server.getLastCheckAt());
                    mcpVOs.add(mcpVO);
                }
            }
            vo.setMcpBindings(mcpVOs);
        }

        // 查询知识库绑定
        List<AgentKnowledgeBinding> kbBindings = agentKnowledgeBindingMapper.selectList(
                new LambdaQueryWrapper<AgentKnowledgeBinding>().eq(AgentKnowledgeBinding::getAgentId, agent.getId()));
        if (!kbBindings.isEmpty()) {
            List<KnowledgeBaseVO> kbVOs = new java.util.ArrayList<>();
            for (AgentKnowledgeBinding kbBinding : kbBindings) {
                KnowledgeBaseVO kbVO = new KnowledgeBaseVO();
                kbVO.setId(kbBinding.getKbId());
                kbVO.setAlias(kbBinding.getAlias() != null ? kbBinding.getAlias() : kbBinding.getKbId());
                kbVOs.add(kbVO);
            }
            vo.setKnowledgeBases(kbVOs);
        }

        return vo;
    }
}
