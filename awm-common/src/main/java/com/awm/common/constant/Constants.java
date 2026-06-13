package com.awm.common.constant;

public final class Constants {

    private Constants() {}

    // Agent Lifecycle Status
    public static final String AGENT_LIFECYCLE_DRAFT = "DRAFT";
    public static final String AGENT_LIFECYCLE_ACTIVE = "ACTIVE";
    public static final String AGENT_LIFECYCLE_INACTIVE = "INACTIVE";
    public static final String AGENT_LIFECYCLE_ARCHIVED = "ARCHIVED";

    // Agent Runtime Status
    public static final String AGENT_RUNTIME_ONLINE = "ONLINE";
    public static final String AGENT_RUNTIME_BUSY = "BUSY";
    public static final String AGENT_RUNTIME_OFFLINE = "OFFLINE";
    public static final String AGENT_RUNTIME_ERROR = "ERROR";

    // Task Status
    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_RUNNING = "RUNNING";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String TASK_STATUS_FAILED = "FAILED";
    public static final String TASK_STATUS_CANCELLED = "CANCELLED";

    // Message Type
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_SYSTEM = "system";
    public static final String MESSAGE_TYPE_TOOL_CALL = "tool_call";
    public static final String MESSAGE_TYPE_TOOL_RESULT = "tool_result";

    // Sender Type
    public static final String SENDER_TYPE_AGENT = "AGENT";
    public static final String SENDER_TYPE_USER = "USER";
    public static final String SENDER_TYPE_SYSTEM = "SYSTEM";

    // Memory Type
    public static final String MEMORY_TYPE_SHORT_TERM = "SHORT_TERM";
    public static final String MEMORY_TYPE_LONG_TERM = "LONG_TERM";
    public static final String MEMORY_TYPE_EPISODIC = "EPISODIC";

    // MCP Health Status
    public static final String MCP_HEALTH_HEALTHY = "HEALTHY";
    public static final String MCP_HEALTH_UNHEALTHY = "UNHEALTHY";
    public static final String MCP_HEALTH_UNKNOWN = "UNKNOWN";

    // Knowledge Index Status
    public static final String KB_INDEX_IDLE = "IDLE";
    public static final String KB_INDEX_INDEXING = "INDEXING";
    public static final String KB_INDEX_COMPLETED = "COMPLETED";
    public static final String KB_INDEX_FAILED = "FAILED";

    // Chat Group Member Role
    public static final String GROUP_ROLE_MANAGER = "MANAGER";
    public static final String GROUP_ROLE_MEMBER = "MEMBER";
}
