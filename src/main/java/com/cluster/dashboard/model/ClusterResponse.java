package com.cluster.dashboard.model;

import java.util.List;

public record ClusterResponse(
        String clusterName,
        String kubernetesVersion,
        int totalNodes,
        int readyNodes,
        int notReadyNodes,
        int totalNamespaces,
        int totalPods,
        int runningPods,
        int pendingPods,
        int failedPods,
        List<NodeSummary> nodes,
        ResourceUsage cpu,
        ResourceUsage memory
) {
}