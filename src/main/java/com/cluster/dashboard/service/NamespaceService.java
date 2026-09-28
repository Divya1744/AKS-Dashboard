package com.cluster.dashboard.service;

import com.cluster.dashboard.model.NamespaceResponse;
import com.cluster.dashboard.model.NamespaceSummary;
import com.cluster.dashboard.model.PodSummary;
import com.cluster.dashboard.model.ResourceUsage;
import com.cluster.dashboard.util.ResourceParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.custom.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NamespaceService {

    private final KubernetesApiService kubernetesApiService;

    private final MetricsApiService metricsApiService;

    private final PodService podService;

    public NamespaceResponse getNamespaceOverview(
            String namespace) {

        /*
         * Kubernetes API:
         *     1 call
         *
         * Metrics API:
         *     1 call
         */

        V1PodList pods =
                kubernetesApiService.getPods(
                        namespace
                );

        JsonNode metrics =
                metricsApiService.getPodMetrics(
                        namespace
                );

        ResourceTotals totals =
                calculateResourceTotals(
                        pods,
                        metrics
                );

        List<PodSummary> podSummaries =
                podService.createPodSummaries(
                        pods,
                        metrics
                );

        ResourceUsage cpu =
                new ResourceUsage(
                        ResourceParser.cpuToString(
                                totals.cpuUsage()
                        ),
                        ResourceParser.cpuToString(
                                totals.cpuRequest()
                        ),
                        ResourceParser.cpuToString(
                                totals.cpuLimit()
                        ),
                        ResourceParser.utilization(
                                totals.cpuUsage(),
                                totals.cpuLimit()
                        )
                );

        ResourceUsage memory =
                new ResourceUsage(
                        ResourceParser.memoryToGi(
                                totals.memoryUsage()
                        ),
                        ResourceParser.memoryToGi(
                                totals.memoryRequest()
                        ),
                        ResourceParser.memoryToGi(
                                totals.memoryLimit()
                        ),
                        ResourceParser.utilization(
                                totals.memoryUsage(),
                                totals.memoryLimit()
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
            V1PodList pods) {

        if (pods == null ||
                pods.getItems() == null) {

            return "Unknown";
        }

        for (V1Pod pod :
                pods.getItems()) {

            if (pod.getMetadata() == null ||
                    pod.getMetadata().getLabels() == null) {

                continue;
            }

            String release =
                    pod.getMetadata()
                            .getLabels()
                            .get("release");

            if (release != null) {
                return release;
            }
        }

        return "Unknown";
    }

    private String findIdentityProvider(
            V1PodList pods) {

        if (pods == null ||
                pods.getItems() == null) {

            return "azureAD";
        }

        for (V1Pod pod :
                pods.getItems()) {

            if (pod.getMetadata() == null) {
                continue;
            }

            String podName =
                    pod.getMetadata().getName();

            if (podName != null && (podName.contains("-pub-ms-urel-")) || podName.contains("publishing-method-server")) {

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

    private ResourceTotals calculateResourceTotals(
            V1PodList pods,
            JsonNode metrics) {

        long cpuRequest = 0;
        long cpuLimit = 0;

        long memoryRequest = 0;
        long memoryLimit = 0;

        long cpuUsage = 0;
        long memoryUsage = 0;

        /*
         * Requests and limits
         * come from Pod specifications.
         */

        if (pods != null &&
                pods.getItems() != null) {

            for (V1Pod pod :
                    pods.getItems()) {

                if (pod.getSpec() == null ||
                        pod.getSpec()
                                .getContainers() == null) {

                    continue;
                }

                for (V1Container container :
                        pod.getSpec()
                                .getContainers()) {

                    V1ResourceRequirements resources = container.getResources();    //the resource property in spec.containers

                    if (resources == null) {
                        continue;
                    }

                    Map<String, Quantity> requests = resources.getRequests();

                    Map<String, Quantity> limits = resources.getLimits();

                    if (requests != null) {

                        Quantity cpu = requests.get("cpu");

                        Quantity memory = requests.get("memory");

                        if (cpu != null) {

                            cpuRequest += ResourceParser.parseCpu(cpu);
                        }

                        if (memory != null) {

                            memoryRequest += ResourceParser.parseMemory(memory);
                        }
                    }

                    if (limits != null) {

                        Quantity cpu = limits.get("cpu");

                        Quantity memory = limits.get("memory");

                        if (cpu != null) {

                            cpuLimit += ResourceParser.parseCpu(cpu);
                        }

                        if (memory != null) {

                            memoryLimit += ResourceParser.parseMemory(memory);
                        }
                    }
                }
            }
        }

        /*
         * Actual usage comes from Metrics API.
         */

        if (metrics != null &&
                metrics.has("items")) {

            for (JsonNode pod :
                    metrics.get("items")) {

                if (!pod.has("containers")) {
                    continue;
                }

                for (JsonNode container :
                        pod.get("containers")) {

                    if (!container.has("usage")) {
                        continue;
                    }

                    JsonNode usage =
                            container.get("usage");

                    if (usage.has("cpu")) {

                        cpuUsage +=
                                ResourceParser
                                        .parseCpuToMilli(
                                                usage.get("cpu")
                                                        .asText()
                                        );
                    }

                    if (usage.has("memory")) {

                        memoryUsage +=
                                ResourceParser
                                        .parseMemoryToKi(
                                                usage.get("memory")
                                                        .asText()
                                        );
                    }
                }
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

        V1NamespaceList namespaces =
                kubernetesApiService
                        .getNamespaces();

        List<NamespaceSummary> result =
                new ArrayList<>();

        if (namespaces == null ||
                namespaces.getItems() == null) {

            return result;
        }

        for (V1Namespace namespace :
                namespaces.getItems()) {

            String name =
                    namespace.getMetadata()
                            .getName();

            result.add(
                    new NamespaceSummary(name)
            );
        }

        return result;
    }
}