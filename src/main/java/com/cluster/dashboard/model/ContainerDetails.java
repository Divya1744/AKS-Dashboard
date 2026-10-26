package com.cluster.dashboard.model;

public record ContainerDetails(
        String name,
        String image,
        boolean isInit,
        ResourceUsage cpu,
        ResourceUsage memory
) {
}