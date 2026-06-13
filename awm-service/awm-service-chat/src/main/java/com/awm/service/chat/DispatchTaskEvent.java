package com.awm.service.chat;

import org.springframework.context.ApplicationEvent;

public class DispatchTaskEvent extends ApplicationEvent {

    private final String groupId;
    private final String message;

    public DispatchTaskEvent(String groupId, String message) {
        super(groupId);
        this.groupId = groupId;
        this.message = message;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getMessage() {
        return message;
    }
}
