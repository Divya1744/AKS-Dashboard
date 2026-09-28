package com.cluster.dashboard.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KubernetesClientTestService {

    private final ApiClient apiClient;

    public void testConnection() {

        CoreV1Api api =
                new CoreV1Api(apiClient);

        try {

            V1PodList podList =
                    api.listNamespacedPod(
                            "rd-us-aroy"
                    ).execute();

            for (V1Pod pod :
                    podList.getItems()) {

                System.out.println(
                        pod.getMetadata().getName()
                );
            }

        } catch (ApiException e) {

            System.out.println(
                    "Kubernetes API error: "
                            + e.getCode()
                            + " "
                            + e.getResponseBody()
            );
        }
    }
}