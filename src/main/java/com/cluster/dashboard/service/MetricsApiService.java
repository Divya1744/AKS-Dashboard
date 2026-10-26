package com.cluster.dashboard.service;

import com.cluster.dashboard.config.SessionKubernetesClientProvider;
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

    private static final String METRICS_GROUP = "metrics.k8s.io";

    private static final String METRICS_VERSION = "v1beta1";

    //private static final String POD_RESOURCE = "pods";

    //private final ApiClient apiClient;
    private final SessionKubernetesClientProvider clientProvider;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public JsonNode getPodMetrics(String namespace) {

        //CustomObjectsApi api = new CustomObjectsApi(apiClient);
        CustomObjectsApi api =
                new CustomObjectsApi(clientProvider.getApiClient());

        try {

            Object response =
                    api.listNamespacedCustomObject(
                            METRICS_GROUP,
                            METRICS_VERSION,
                            namespace,
                            "pods"
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

    public JsonNode getNodeMetrics() {

        CustomObjectsApi api =
                new CustomObjectsApi(clientProvider.getApiClient());

        try {

            Object response =
                    api.listClusterCustomObject(
                            METRICS_GROUP,
                            METRICS_VERSION,
                            "nodes"
                    ).execute();

            return objectMapper.valueToTree(response);

        } catch (ApiException e) {

            throw new RuntimeException(
                    "Failed to get Node metrics from Kubernetes Metrics API. "
                            + "HTTP " + e.getCode()
                            + ": " + e.getResponseBody(), e);
        }
    }

}