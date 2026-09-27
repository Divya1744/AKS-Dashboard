package com.cluster.dashboard.model;

public record ResourceUsage(
        String usage,
        String request,
        String limit,
        double utilization
) {
}