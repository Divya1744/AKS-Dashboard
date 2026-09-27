package com.cluster.dashboard.model;

import java.util.List;

public record PodDetails(
        String name,
        String status,
        String age,
        String node,
        int restarts,
        List<ContainerDetails> containers
) {
}