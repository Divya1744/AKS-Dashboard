package com.cluster.dashboard.model;

public record KubeconfigUploadResponse(
        String status,
        String code,
        String message,
        String clusterName
) {
}