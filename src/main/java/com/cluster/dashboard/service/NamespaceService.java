package com.cluster.dashboard.service;

import com.cluster.dashboard.model.NamespaceResponse;
import com.cluster.dashboard.model.NamespaceSummary;
import com.cluster.dashboard.model.PodSummary;
import com.cluster.dashboard.model.ResourceUsage;
import com.cluster.dashboard.util.ResourceParser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NamespaceService {

    //private static final String NAMESPACE ="rd-us-dsivasubramanian";

    private final KubernetesApiService kubernetesApiService;
    private final MetricsApiService metricsApiService;
    private final PodService podService;

    public NamespaceResponse getNamespaceOverview(String namespace) {

        /*
         * External API calls:
         *
         * 1. Kubernetes API
         * 2. Metrics API
         *
         * Each is called only once.
         */

        JsonNode pods = kubernetesApiService.getPods(namespace);

        JsonNode metrics = metricsApiService.getPodMetrics(namespace);

        ResourceTotals totals = calculateResourceTotals(pods, metrics);

        List<PodSummary> podSummaries =
                podService.createPodSummaries(
                        pods, metrics
                );

        ResourceUsage cpu =
                new ResourceUsage(
                        ResourceParser.cpuToString(
                                totals.cpuUsage
                        ),
                        ResourceParser.cpuToString(
                                totals.cpuRequest
                        ),
                        ResourceParser.cpuToString(
                                totals.cpuLimit
                        ),
                        ResourceParser.utilization(
                                totals.cpuUsage,
                                totals.cpuLimit
                        )
                );

        ResourceUsage memory =
                new ResourceUsage(
                        ResourceParser.memoryToGi(
                                totals.memoryUsage
                        ),
                        ResourceParser.memoryToGi(
                                totals.memoryRequest
                        ),
                        ResourceParser.memoryToGi(
                                totals.memoryLimit
                        ),
                        ResourceParser.utilization(
                                totals.memoryUsage,
                                totals.memoryLimit
                        )
                );

        String releaseName =
                findReleaseName(pods);

        String identityProvider =
                findIdentityProvider(pods);

        String windchillUrl =
                buildWindchillUrl(namespace);

        return new NamespaceResponse(
                namespace,
                releaseName,
                identityProvider,
                windchillUrl,
                podSummaries.size(),
                cpu,
                memory,
                podSummaries
        );
    }

    private String findReleaseName(
            JsonNode pods) {

        for (JsonNode pod :
                pods.get("items")) {

            JsonNode labels =
                    pod.get("metadata")
                            .get("labels");

            if (labels != null &&
                    labels.has("release")) {

                return labels
                        .get("release")
                        .asText();
            }
        }

        return "Unknown";
    }

    private String findIdentityProvider(
            JsonNode pods) {

        for (JsonNode pod :
                pods.get("items")) {

            String podName =
                    pod.get("metadata")
                            .get("name")
                            .asText();

            if (podName.contains(
                    "-pub-ms-urel-")) {

                return "wncsaas";
            }
        }

        return "azureAD";
    }

    private String buildWindchillUrl(
            String namespace) {

        return "http://"
                + namespace
                + ".rd-us.azure.ptc.com/windchill";
    }

    // Existing calculateResourceTotals() stays here
    // Existing ResourceTotals record stays here


    private ResourceTotals calculateResourceTotals(JsonNode pods, JsonNode metrics) {

        long cpuRequest = 0;
        long cpuLimit = 0;

        long memoryRequest = 0;
        long memoryLimit = 0;

        long cpuUsage = 0;
        long memoryUsage = 0;

        /*
         * Calculate requests and limits
         * from Kubernetes Pod specifications.
         */

        for (JsonNode pod : pods.get("items")) {

            for (JsonNode container : pod.get("spec").get("containers")) {

                JsonNode resources = container.get("resources");

                if (resources == null) {
                    continue;
                }

                JsonNode requests =
                        resources.get("requests");

                JsonNode limits =
                        resources.get("limits");

                if (requests != null) {

                    if (requests.has("cpu")) {

                        cpuRequest +=
                                ResourceParser
                                        .parseCpuToMilli(
                                                requests
                                                        .get("cpu")
                                                        .asText()
                                        );
                    }

                    if (requests.has("memory")) {

                        memoryRequest +=
                                ResourceParser
                                        .parseMemoryToKi(
                                                requests
                                                        .get("memory")
                                                        .asText()
                                        );
                    }
                }

                if (limits != null) {

                    if (limits.has("cpu")) {

                        cpuLimit +=
                                ResourceParser
                                        .parseCpuToMilli(
                                                limits
                                                        .get("cpu")
                                                        .asText()
                                        );
                    }

                    if (limits.has("memory")) {

                        memoryLimit +=
                                ResourceParser
                                        .parseMemoryToKi(
                                                limits
                                                        .get("memory")
                                                        .asText()
                                        );
                    }
                }
            }
        }

        /*
         * Calculate actual usage
         * from Metrics API.
         */

        for (JsonNode pod : metrics.get("items")) {

            for (JsonNode container : pod.get("containers")) {

                cpuUsage += ResourceParser.parseCpuToMilli(container
                                                .get("usage")
                                                .get("cpu")
                                                .asText());

                memoryUsage +=
                        ResourceParser
                                .parseMemoryToKi(
                                        container
                                                .get("usage")
                                                .get("memory")
                                                .asText()
                                );
            }
        }

        return new ResourceTotals(
                cpuRequest,
                cpuLimit,
                memoryRequest,
                memoryLimit,
                cpuUsage,
                memoryUsage
        );
    }

    private record ResourceTotals(
            long cpuRequest,
            long cpuLimit,
            long memoryRequest,
            long memoryLimit,
            long cpuUsage,
            long memoryUsage
    ) {
    }

    public List<NamespaceSummary> getNamespaces() {

        JsonNode namespaces =
                kubernetesApiService.getNamespaces();

        List<NamespaceSummary> result =
                new ArrayList<>();

        for (JsonNode namespace :
                namespaces.get("items")) {

            String name =
                    namespace
                            .get("metadata")
                            .get("name")
                            .asText();

            result.add(
                    new NamespaceSummary(name)
            );
        }

        return result;
    }
}