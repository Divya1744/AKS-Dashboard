package com.cluster.dashboard.model;

import java.util.List;

public record NamespaceResponse(
        String name,
        String releaseName,
        String identityProvider,
        String windchillUrl,
        int podCount,
        ResourceUsage cpu,
        ResourceUsage memory,
        List<PodSummary> pods
) {
}