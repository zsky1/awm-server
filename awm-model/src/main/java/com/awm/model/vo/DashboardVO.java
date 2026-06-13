package com.awm.model.vo;

import lombok.Data;

@Data
public class DashboardVO {
    private int totalAgents;
    private int onlineAgents;
    private int busyAgents;
    private int errorAgents;
    private int offlineAgents;
    private int pendingTasks;
    private int inProgressTasks;
    private int completedTasks;
    private int failedTasks;
    private int totalMcpServers;
    private int healthyMcpServers;
    private int unhealthyMcpServers;
}
