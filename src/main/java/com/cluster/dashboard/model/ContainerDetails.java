package com.cluster.dashboard.model;

public record ContainerDetails(
        String name,
        String image,
        ResourceUsage cpu,
        ResourceUsage memory
) {
}