package com.cluster.dashboard.model;

import java.util.List;

public record PodSummary(
        String name,
        String status,
        String ready,
        String age,
        String node,
        int restarts,
        List<ContainerDetails> containers
) {
}