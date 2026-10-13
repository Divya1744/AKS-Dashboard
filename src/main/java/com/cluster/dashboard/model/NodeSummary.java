package com.cluster.dashboard.model;

public record NodeSummary(
        String name,
        String status,
        int podCount,
        String cpuUsage,
        String cpuCapacity,
        String cpuAllocatable,
        String memoryUsage,
        String memoryCapacity,
        String memoryAllocatable
) {
}