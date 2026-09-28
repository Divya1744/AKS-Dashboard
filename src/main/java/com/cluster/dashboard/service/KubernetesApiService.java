package com.cluster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KubernetesApiService {

    private final ApiClient apiClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public V1PodList getPods(String namespace) {

        CoreV1Api api =
                new CoreV1Api(apiClient);

        try {

            return api.listNamespacedPod(
                    namespace
            ).execute();

        } catch (ApiException e) {

            throw new RuntimeException(
                    "Failed to get Pods from Kubernetes API. "
                            + "HTTP "
                            + e.getCode()
                            + ": "
                            + e.getResponseBody(),
                    e
            );
        }
    }

    public V1NamespaceList getNamespaces() {

        CoreV1Api api =
                new CoreV1Api(apiClient);

        try {

            return api.listNamespace()
                    .execute();

        } catch (ApiException e) {

            throw new RuntimeException(
                    "Failed to get Namespaces from Kubernetes API. "
                            + "HTTP "
                            + e.getCode()
                            + ": "
                            + e.getResponseBody(),
                    e
            );
        }
    }

    /*
     * Kept as a helper in case we need JSON conversion
     * for Kubernetes API responses later.
     */
    public JsonNode toJson(Object object) {

        return objectMapper.valueToTree(object);
    }
}