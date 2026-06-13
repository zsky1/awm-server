package com.awm.service.modelconfig;

import com.awm.dal.mapper.ModelConfigMapper;
import com.awm.model.entity.ModelConfig;
import com.awm.model.vo.ModelConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;

    public List<ModelConfigVO> listModelConfigs() {
        List<ModelConfig> configs = modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfig>().orderByDesc(ModelConfig::getCreatedAt));
        return configs.stream().map(this::toVO).collect(Collectors.toList());
    }

    public ModelConfigVO getModelConfigById(String id) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("ModelConfig not found: " + id);
        }
        return toVO(config);
    }

    @Transactional
    public ModelConfigVO createModelConfig(ModelConfig config) {
        if (config.getIsDefault() != null && config.getIsDefault()) {
            clearDefaultFlag();
        }
        modelConfigMapper.insert(config);
        return toVO(config);
    }

    @Transactional
    public ModelConfigVO updateModelConfig(String id, ModelConfig config) {
        ModelConfig existing = modelConfigMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("ModelConfig not found: " + id);
        }
        if (config.getIsDefault() != null && config.getIsDefault() && !Boolean.TRUE.equals(existing.getIsDefault())) {
            clearDefaultFlag();
        }
        if (config.getName() != null) existing.setName(config.getName());
        if (config.getProvider() != null) existing.setProvider(config.getProvider());
        if (config.getModel() != null) existing.setModel(config.getModel());
        if (config.getApiKey() != null) existing.setApiKey(config.getApiKey());
        if (config.getBaseUrl() != null) existing.setBaseUrl(config.getBaseUrl());
        if (config.getTemperature() != null) existing.setTemperature(config.getTemperature());
        if (config.getMaxTokens() != null) existing.setMaxTokens(config.getMaxTokens());
        if (config.getIsDefault() != null) existing.setIsDefault(config.getIsDefault());
        if (config.getDescription() != null) existing.setDescription(config.getDescription());
        modelConfigMapper.updateById(existing);
        return toVO(existing);
    }

    @Transactional
    public void deleteModelConfig(String id) {
        modelConfigMapper.deleteById(id);
    }

    private void clearDefaultFlag() {
        List<ModelConfig> defaults = modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getIsDefault, true));
        for (ModelConfig config : defaults) {
            config.setIsDefault(false);
            modelConfigMapper.updateById(config);
        }
    }

    private ModelConfigVO toVO(ModelConfig config) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setId(config.getId());
        vo.setName(config.getName());
        vo.setProvider(config.getProvider());
        vo.setModel(config.getModel());
        vo.setApiKey(config.getApiKey());
        vo.setBaseUrl(config.getBaseUrl());
        vo.setTemperature(config.getTemperature());
        vo.setMaxTokens(config.getMaxTokens());
        vo.setIsDefault(config.getIsDefault());
        vo.setDescription(config.getDescription());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }
}
