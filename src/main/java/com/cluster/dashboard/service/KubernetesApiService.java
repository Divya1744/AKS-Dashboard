package com.cluster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.VersionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KubernetesApiService {

    private final ApiClient apiClient;
    private static final String WINDCHILL_SERVICE = "ms-service-windchill-0";

    private static final String MAPPING_GROUP = "getambassador.io";
    private static final String MAPPING_VERSION = "v2";
    private static final String MAPPING_PLURAL = "mappings";

    public String getWindchillUrl(String namespace) {

        if (!hasWindchillService(namespace)) {
            return null;
        }

        String hostname = getWindchillHostname(namespace);

        if (hostname == null || hostname.isBlank()) {
            return null;
        }

        return "https://" + hostname + "/Windchill/app/";
    }

    private boolean hasWindchillService(String namespace) {

        CoreV1Api api = new CoreV1Api(apiClient);

        try {
            api.readNamespacedService(
                    WINDCHILL_SERVICE,
                    namespace
            ).execute();

            return true;

        } catch (ApiException e) {

            if (e.getCode() == 404) {
                return false;
            }

            throw new RuntimeException(
                    "Failed to check Windchill Service in namespace "
                            + namespace
                            + ". HTTP "
                            + e.getCode()
                            + ": "
                            + e.getResponseBody(),
                    e
            );
        }
    }

    private String getWindchillHostname(String namespace) {

        CustomObjectsApi api = new CustomObjectsApi(apiClient);

        try {
            Object response = api.listNamespacedCustomObject(
                    MAPPING_GROUP,
                    MAPPING_VERSION,
                    namespace,
                    MAPPING_PLURAL
            ).execute();

            ObjectMapper mapper = new ObjectMapper();

            JsonNode mappings = mapper
                    .valueToTree(response)
                    .path("items");

            for (JsonNode mapping : mappings) {

                JsonNode spec = mapping.path("spec");

                String service = spec
                        .path("service")
                        .asText("");

                String hostname = spec
                        .path("host")
                        .asText("");

                boolean matchesService =
                        service.equals(WINDCHILL_SERVICE)
                                || service.startsWith(
                                WINDCHILL_SERVICE + "." + namespace + ":"
                        )
                                || service.startsWith(
                                WINDCHILL_SERVICE + "." + namespace + "."
                        );

                if (matchesService && !hostname.isBlank()) {
                    return hostname;
                }
            }

            return null;

        } catch (ApiException e) {

            throw new RuntimeException(
                    "Failed to retrieve Ambassador Mappings for namespace "
                            + namespace
                            + ". HTTP "
                            + e.getCode()
                            + ": "
                            + e.getResponseBody(),
                    e
            );
        }
    }


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


    public V1NodeList getNodes() {

        CoreV1Api api = new CoreV1Api(apiClient);

        try {
            return api.listNode().execute();

        } catch (ApiException e) {
            throw new RuntimeException(
                    "Failed to get Nodes from Kubernetes API. "
                            + "HTTP " + e.getCode() + ": "
                            + e.getResponseBody(), e);
        }
    }

    public V1PodList getAllPods() {

        CoreV1Api api = new CoreV1Api(apiClient);

        try {
            return api.listPodForAllNamespaces().execute();

        } catch (ApiException e) {
            throw new RuntimeException(
                    "Failed to get Pods from Kubernetes API. "
                            + "HTTP " + e.getCode() + ": "
                            + e.getResponseBody(), e);
        }
    }

    public String getKubernetesVersion() {

        VersionApi api = new VersionApi(apiClient);

        try {
            VersionInfo version = api.getCode().execute();

            return version.getGitVersion();

        } catch (ApiException e) {
            throw new RuntimeException(
                    "Failed to get Kubernetes version. "
                            + "HTTP " + e.getCode() + ": "
                            + e.getResponseBody(), e);
        }
    }

    public String getContainerLogs(
            String namespace,
            String podName,
            String containerName,
            Integer tailLines) {

        CoreV1Api api = new CoreV1Api(apiClient);

        try {
            return api.readNamespacedPodLog(podName, namespace)
                    .container(containerName)
                    .tailLines(tailLines)
                    .timestamps(true)
                    .execute();

        } catch (ApiException e) {
            throw new RuntimeException(
                    "Failed to get container logs. "
                            + "HTTP " + e.getCode()
                            + ": " + e.getResponseBody(), e);
        }
    }
}