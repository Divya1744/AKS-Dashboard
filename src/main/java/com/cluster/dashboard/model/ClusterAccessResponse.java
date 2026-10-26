
package com.cluster.dashboard.model;

public record ClusterAccessResponse(
        String status,
        String code,
        String message,
        String clusterName
) {
}
