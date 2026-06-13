package com.awm.web.controller;

import com.awm.common.result.Result;
import com.awm.model.dto.ModelConfigCreateDTO;
import com.awm.model.dto.ModelConfigUpdateDTO;
import com.awm.model.vo.ModelConfigVO;
import com.awm.service.modelconfig.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/model-configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping
    public Result<List<ModelConfigVO>> list() {
        return Result.success(modelConfigService.listModelConfigs());
    }

    @GetMapping("/{id}")
    public Result<ModelConfigVO> get(@PathVariable String id) {
        return Result.success(modelConfigService.getModelConfigById(id));
    }

    @PostMapping
    public Result<ModelConfigVO> create(@RequestBody @Valid ModelConfigCreateDTO dto) {
        com.awm.model.entity.ModelConfig config = new com.awm.model.entity.ModelConfig();
        config.setName(dto.getName());
        config.setProvider(dto.getProvider());
        config.setModel(dto.getModel());
        config.setApiKey(dto.getApiKey());
        config.setBaseUrl(dto.getBaseUrl());
        config.setTemperature(dto.getTemperature());
        config.setMaxTokens(dto.getMaxTokens());
        config.setIsDefault(dto.getIsDefault());
        config.setDescription(dto.getDescription());
        return Result.success(modelConfigService.createModelConfig(config));
    }

    @PutMapping("/{id}")
    public Result<ModelConfigVO> update(@PathVariable String id, @RequestBody @Valid ModelConfigUpdateDTO dto) {
        com.awm.model.entity.ModelConfig config = new com.awm.model.entity.ModelConfig();
        config.setName(dto.getName());
        config.setProvider(dto.getProvider());
        config.setModel(dto.getModel());
        config.setApiKey(dto.getApiKey());
        config.setBaseUrl(dto.getBaseUrl());
        config.setTemperature(dto.getTemperature());
        config.setMaxTokens(dto.getMaxTokens());
        config.setIsDefault(dto.getIsDefault());
        config.setDescription(dto.getDescription());
        return Result.success(modelConfigService.updateModelConfig(id, config));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        modelConfigService.deleteModelConfig(id);
        return Result.success();
    }
}
