package com.awm.ai.model;

import java.util.Map;

/**
 * 工具定义（Function Calling）
 */
public record ToolDefinition(
    String name,
    String description,
    Map<String, Object> parameters
) {}
