package com.cluster.dashboard.service;

import com.cluster.dashboard.model.ContainerDetails;
import com.cluster.dashboard.model.PodSummary;
import com.cluster.dashboard.model.ResourceUsage;
import com.cluster.dashboard.util.ResourceParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PodService {

    public List<PodSummary> createPodSummaries(
            V1PodList pods,
            JsonNode metrics) {

        List<PodSummary> result =
                new ArrayList<>();

        if (pods == null ||
                pods.getItems() == null) {

            return result;
        }

        for (V1Pod pod :
                pods.getItems()) {

            JsonNode podMetrics =
                    findPodMetrics(
                            metrics,
                            pod.getMetadata().getName()
                    );

            result.add(
                    createPodSummary(
                            pod,
                            podMetrics
                    )
            );
        }

        return result;
    }

    public PodSummary createPodSummary(
            V1Pod pod,
            JsonNode metrics) {

        String name =
                pod.getMetadata().getName();

        String status =
                pod.getStatus() != null
                        ? pod.getStatus().getPhase()
                        : "Unknown";

        String ready = getReadyState(pod);

        String node =
                pod.getSpec() != null
                        ? pod.getSpec().getNodeName()
                        : null;

        String age = calculateAge(
                pod.getMetadata().getCreationTimestamp()
        );

        int restarts =
                getRestartCount(pod);

        List<ContainerDetails> containers =
                createContainerDetails(
                        pod,
                        metrics
                );

        return new PodSummary(
                name,
                status,
                ready,
                age,
                node,
                restarts,
                containers
        );
    }

    private List<ContainerDetails> createContainerDetails(
            V1Pod pod,
            JsonNode metrics) {

        List<ContainerDetails> result =
                new ArrayList<>();

        if (pod.getSpec() == null) {
            return result;
        }

        // Normal containers
        if (pod.getSpec().getContainers() != null) {

            for (V1Container container :
                    pod.getSpec().getContainers()) {

                String name =
                        container.getName();

                String image =
                        getImageTag(container.getImage());

                long cpuRequest = 0;
                long cpuLimit = 0;

                long memoryRequest = 0;
                long memoryLimit = 0;

                V1ResourceRequirements resources =
                        container.getResources();

                if (resources != null) {

                    Map<String, Quantity> requests =
                            resources.getRequests();

                    Map<String, Quantity> limits =
                            resources.getLimits();

                    if (requests != null) {

                        cpuRequest =
                                parseCpu(requests.get("cpu"));

                        memoryRequest =
                                parseMemory(requests.get("memory"));
                    }

                    if (limits != null) {

                        cpuLimit =
                                parseCpu(limits.get("cpu"));

                        memoryLimit =
                                parseMemory(limits.get("memory"));
                    }
                }

                JsonNode metricContainer =
                        findMetricContainer(
                                metrics,
                                name
                        );

                long cpuUsage =
                        getCpuUsage(metricContainer);

                long memoryUsage =
                        getMemoryUsage(metricContainer);

                ResourceUsage cpu =
                        new ResourceUsage(
                                ResourceParser.cpuToString(
                                        cpuUsage
                                ),
                                ResourceParser.cpuToString(
                                        cpuRequest
                                ),
                                ResourceParser.cpuToString(
                                        cpuLimit
                                ),
                                ResourceParser.utilization(
                                        cpuUsage,
                                        cpuLimit
                                )
                        );

                ResourceUsage memory =
                        new ResourceUsage(
                                ResourceParser.memoryToGi(
                                        memoryUsage
                                ),
                                ResourceParser.memoryToGi(
                                        memoryRequest
                                ),
                                ResourceParser.memoryToGi(
                                        memoryLimit
                                ),
                                ResourceParser.utilization(
                                        memoryUsage,
                                        memoryLimit
                                )
                        );

                result.add(
                        new ContainerDetails(
                                name,
                                image,
                                false,
                                cpu,
                                memory
                        )
                );
            }
        }

        // Init containers - no historical data available in metrics api to get init-container details.
        if (pod.getSpec().getInitContainers() != null) {

            for (V1Container container :
                    pod.getSpec().getInitContainers()) {

                String name =
                        container.getName();

                String image =
                        getImageTag(container.getImage());

                long cpuRequest = 0;
                long cpuLimit = 0;

                long memoryRequest = 0;
                long memoryLimit = 0;

                V1ResourceRequirements resources =
                        container.getResources();

                if (resources != null) {

                    Map<String, Quantity> requests =
                            resources.getRequests();

                    Map<String, Quantity> limits =
                            resources.getLimits();

                    if (requests != null) {

                        cpuRequest =
                                parseCpu(requests.get("cpu"));

                        memoryRequest =
                                parseMemory(requests.get("memory"));
                    }

                    if (limits != null) {

                        cpuLimit =
                                parseCpu(limits.get("cpu"));

                        memoryLimit =
                                parseMemory(limits.get("memory"));
                    }
                }

                /*
                 * Init containers normally don't have
                 * current metrics after they finish.
                 */

                JsonNode metricContainer =
                        findMetricContainer(metrics, name);

                long cpuUsage =
                        getCpuUsage(metricContainer);

                long memoryUsage =
                        getMemoryUsage(metricContainer);



                ResourceUsage cpu =
                        new ResourceUsage(
                                ResourceParser.cpuToString(cpuUsage),
                                ResourceParser.cpuToString(cpuRequest),
                                ResourceParser.cpuToString(cpuLimit),
                                ResourceParser.utilization(
                                        cpuUsage,
                                        cpuLimit
                                )
                        );

                ResourceUsage memory =
                        new ResourceUsage(
                                ResourceParser.memoryToGi(memoryUsage),
                                ResourceParser.memoryToGi(memoryRequest),
                                ResourceParser.memoryToGi(memoryLimit),
                                ResourceParser.utilization(
                                        memoryUsage,
                                        memoryLimit
                                )
                        );

                result.add(
                        new ContainerDetails(
                                name,
                                image,
                                true,
                                cpu,
                                memory
                        )
                );
            }
        }

        return result;
    }

    private long parseCpu(Quantity quantity) {
        return ResourceParser.parseCpu(quantity);
    }

    private long parseMemory(Quantity quantity) {
        return ResourceParser.parseMemory(quantity);
    }

    private long getCpuUsage(
            JsonNode container) {

        if (container == null ||
                !container.has("usage") ||
                !container.get("usage")
                        .has("cpu")) {

            return 0;
        }

        return ResourceParser.parseCpuToMilli(
                container.get("usage")
                        .get("cpu")
                        .asText()
        );
    }

    private long getMemoryUsage(
            JsonNode container) {

        if (container == null ||
                !container.has("usage") ||
                !container.get("usage")
                        .has("memory")) {

            return 0;
        }

        return ResourceParser.parseMemoryToKi(
                container.get("usage")
                        .get("memory")
                        .asText()
        );
    }

    private int getRestartCount(
            V1Pod pod) {

        if (pod.getStatus() == null ||
                pod.getStatus()
                        .getContainerStatuses() == null) {

            return 0;
        }

        int total = 0;

        for (V1ContainerStatus status :
                pod.getStatus()
                        .getContainerStatuses()) {

            if (status.getRestartCount() != null) {

                total +=
                        status.getRestartCount();
            }
        }

        return total;
    }

    private JsonNode findPodMetrics(
            JsonNode metrics,
            String podName) {

        if (metrics == null ||
                !metrics.has("items")) {

            return null;
        }

        for (JsonNode pod :
                metrics.get("items")) {

            if (pod.has("metadata") &&
                    pod.get("metadata")
                            .has("name") &&
                    pod.get("metadata")
                            .get("name")
                            .asText()
                            .equals(podName)) {

                return pod;
            }
        }

        return null;
    }

    private JsonNode findMetricContainer(
            JsonNode podMetrics,
            String containerName) {

        if (podMetrics == null ||
                !podMetrics.has("containers")) {

            return null;
        }

        for (JsonNode container :
                podMetrics.get("containers")) {

            if (container.has("name") &&
                    container.get("name")
                            .asText()
                            .equals(containerName)) {

                return container;
            }
        }

        return null;
    }

    private String calculateAge(
            java.time.OffsetDateTime creationTimestamp) {

        if (creationTimestamp == null) {
            return "Unknown";
        }

        Instant created =
                creationTimestamp.toInstant();

        Duration duration =
                Duration.between(created, Instant.now());

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private String getImageTag(String image) {

        if (image == null || image.isBlank()) {
            return null;
        }

        int colonIndex = image.lastIndexOf(":");

        if (colonIndex == -1) {
            return image;
        }

        return image.substring(colonIndex + 1);
    }

    /** private String getReadyState(V1Pod pod) {

        if (pod.getStatus() == null
                || pod.getStatus().getContainerStatuses() == null) {
            return "0/0";
        }

        int total = pod.getStatus()
                .getContainerStatuses()
                .size();

        int ready = 0;

        for (V1ContainerStatus containerStatus :
                pod.getStatus().getContainerStatuses()) {

            if (Boolean.TRUE.equals(containerStatus.getReady())) {
                ready++;
            }
        }

        return ready + "/" + total;
    } */

    private String getReadyState(V1Pod pod) {

        if (pod.getStatus() == null) {
            return "0/0";
        }

        int total = 0;
        int ready = 0;

        // Count regular containers
        if (pod.getStatus().getContainerStatuses() != null) {

            for (V1ContainerStatus containerStatus :
                    pod.getStatus().getContainerStatuses()) {

                total++;

                if (Boolean.TRUE.equals(containerStatus.getReady())) {
                    ready++;
                }
            }
        }

        // Count restartable init containers (sidecars)
        if (pod.getStatus().getInitContainerStatuses() != null
                && pod.getSpec() != null
                && pod.getSpec().getInitContainers() != null) {

            for (V1ContainerStatus initStatus :
                    pod.getStatus().getInitContainerStatuses()) {

                for (V1Container initContainer :
                        pod.getSpec().getInitContainers()) {

                    if (initContainer.getName().equals(initStatus.getName())
                            && "Always".equals(initContainer.getRestartPolicy())) {

                        total++;

                        if (Boolean.TRUE.equals(initStatus.getReady())) {
                            ready++;
                        }

                        break;
                    }
                }
            }
        }

        return ready + "/" + total;
    }
}