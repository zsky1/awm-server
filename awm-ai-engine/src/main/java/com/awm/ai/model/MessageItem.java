package com.awm.ai.model;

/**
 * 对话消息项
 */
public record MessageItem(
    String role,
    String content
) {

    public static MessageItem system(String content) {
        return new MessageItem("system", content);
    }

    public static MessageItem user(String content) {
        return new MessageItem("user", content);
    }

    public static MessageItem assistant(String content) {
        return new MessageItem("assistant", content);
    }

    public static MessageItem tool(String content) {
        return new MessageItem("tool", content);
    }
}
