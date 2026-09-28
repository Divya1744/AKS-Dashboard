package com.cluster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsApiService {

    private static final String METRICS_GROUP =
            "metrics.k8s.io";

    private static final String METRICS_VERSION =
            "v1beta1";

    private static final String POD_RESOURCE =
            "pods";

    private final ApiClient apiClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public JsonNode getPodMetrics(String namespace) {

        CustomObjectsApi api =
                new CustomObjectsApi(apiClient);

        try {

            Object response =
                    api.listNamespacedCustomObject(
                            METRICS_GROUP,
                            METRICS_VERSION,
                            namespace,
                            POD_RESOURCE
                    ).execute();

            return objectMapper.valueToTree(response);

        } catch (ApiException e) {

            throw new RuntimeException(
                    "Failed to get Pod metrics from Kubernetes Metrics API. "
                            + "HTTP "
                            + e.getCode()
                            + ": "
                            + e.getResponseBody(),
                    e
            );
        }
    }

}