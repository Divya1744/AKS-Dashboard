package com.cluster.dashboard.service;

import com.cluster.dashboard.model.ContainerDetails;
import com.cluster.dashboard.model.PodDetails;
import com.cluster.dashboard.model.PodSummary;
import com.cluster.dashboard.model.ResourceUsage;
import com.cluster.dashboard.util.ResourceParser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PodService {

    public List<PodSummary> createPodSummaries(
            JsonNode pods,
            JsonNode metrics) {

        List<PodSummary> result =
                new ArrayList<>();

        for (JsonNode pod :
                pods.get("items")) {

            String podName =
                    pod.get("metadata")
                            .get("name")
                            .asText();

            JsonNode podMetrics =
                    findPodMetrics(
                            metrics,
                            podName
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
            JsonNode pod,
            JsonNode metrics) {

        String name =
                pod.get("metadata")
                        .get("name")
                        .asText();

        String status =
                pod.get("status")
                        .get("phase")
                        .asText();

        String node =
                pod.get("spec")
                        .get("nodeName")
                        .asText();

        String age =
                calculateAge(
                        pod.get("metadata")
                                .get("creationTimestamp")
                                .asText()
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
                age,
                node,
                restarts,
                containers
        );
    }

    public PodDetails createPodDetails(
            JsonNode pod,
            JsonNode metrics) {

        String name =
                pod.get("metadata")
                        .get("name")
                        .asText();

        String status =
                pod.get("status")
                        .get("phase")
                        .asText();

        String node =
                pod.get("spec")
                        .get("nodeName")
                        .asText();

        String age =
                calculateAge(
                        pod.get("metadata")
                                .get("creationTimestamp")
                                .asText()
                );

        int restarts = getRestartCount(pod);

        List<ContainerDetails> containers = createContainerDetails(pod, metrics);

        System.out.println("Metrics returned to controller: " + metrics);

        return new PodDetails(
                name,
                status,
                age,
                node,
                restarts,
                containers
        );
    }

    private List<ContainerDetails> createContainerDetails(JsonNode pod, JsonNode metrics) {

        List<ContainerDetails> result = new ArrayList<>();

        for (JsonNode container : pod.get("spec").get("containers")) {

            String name =
                    container.get("name")
                            .asText();

            String image =
                    container.get("image")
                            .asText();

            JsonNode resources =
                    container.get("resources");

            long cpuRequest = 0;
            long cpuLimit = 0;

            long memoryRequest = 0;
            long memoryLimit = 0;

            if (resources != null) {

                JsonNode requests =
                        resources.get("requests");

                JsonNode limits =
                        resources.get("limits");

                if (requests != null) {

                    cpuRequest =
                            getCpu(
                                    requests,
                                    "cpu"
                            );

                    memoryRequest =
                            getMemory(
                                    requests,
                                    "memory"
                            );
                }

                if (limits != null) {

                    cpuLimit =
                            getCpu(
                                    limits,
                                    "cpu"
                            );

                    memoryLimit =
                            getMemory(
                                    limits,
                                    "memory"
                            );
                }
            }

            long cpuUsage =
                    getContainerCpuUsage(
                            metrics,
                            name
                    );

            long memoryUsage =
                    getContainerMemoryUsage(
                            metrics,
                            name
                    );

            ResourceUsage cpu =
                    new ResourceUsage(
                            ResourceParser
                                    .cpuToString(
                                            cpuUsage
                                    ),
                            ResourceParser
                                    .cpuToString(
                                            cpuRequest
                                    ),
                            ResourceParser
                                    .cpuToString(
                                            cpuLimit
                                    ),
                            ResourceParser
                                    .utilization(
                                            cpuUsage,
                                            cpuLimit
                                    )
                    );

            ResourceUsage memory =
                    new ResourceUsage(
                            ResourceParser
                                    .memoryToGi(
                                            memoryUsage
                                    ),
                            ResourceParser
                                    .memoryToGi(
                                            memoryRequest
                                    ),
                            ResourceParser
                                    .memoryToGi(
                                            memoryLimit
                                    ),
                            ResourceParser
                                    .utilization(
                                            memoryUsage,
                                            memoryLimit
                                    )
                    );

            result.add(
                    new ContainerDetails(
                            name,
                            image,
                            cpu,
                            memory
                    )
            );
        }

        return result;
    }

    private long getCpu(
            JsonNode resources,
            String field) {

        if (!resources.has(field)) {
            return 0;
        }

        return ResourceParser.parseCpuToMilli(
                resources.get(field).asText()
        );
    }

    private long getMemory(
            JsonNode resources,
            String field) {

        if (!resources.has(field)) {
            return 0;
        }

        return ResourceParser.parseMemoryToKi(
                resources.get(field).asText()
        );
    }

    private int getRestartCount(
            JsonNode pod) {

        JsonNode statuses =
                pod.get("status")
                        .get("containerStatuses");

        if (statuses == null) {
            return 0;
        }

        int total = 0;

        for (JsonNode container :
                statuses) {

            total += container
                    .get("restartCount")
                    .asInt();
        }

        return total;
    }

    private long getContainerCpuUsage(
            JsonNode metrics,
            String containerName) {

        System.out.println(
                "Looking for container: " + containerName
        );

        System.out.println(
                "Metrics JSON: " + metrics
        );

        JsonNode container =
                findMetricContainer(
                        metrics,
                        containerName
                );

        System.out.println(
                "Found container: " + container
        );

        if (container == null) {
            return 0;
        }

        return ResourceParser.parseCpuToMilli(
                container
                        .get("usage")
                        .get("cpu")
                        .asText()
        );
    }

    private long getContainerMemoryUsage(
            JsonNode metrics,
            String containerName) {

        JsonNode container =
                findMetricContainer(
                        metrics,
                        containerName
                );

        if (container == null) {
            return 0;
        }

        return ResourceParser.parseMemoryToKi(
                container
                        .get("usage")
                        .get("memory")
                        .asText()
        );
    }

    private JsonNode findMetricContainer(
            JsonNode metrics,
            String containerName) {

        if (metrics == null ||
                !metrics.has("containers")) {

            return null;
        }

        for (JsonNode container :
                metrics.get("containers")) {

            if (container.get("name")
                    .asText()
                    .equals(containerName)) {

                return container;
            }
        }

        return null;
    }

    private String calculateAge(
            String creationTimestamp) {

        Instant created =
                Instant.parse(
                        creationTimestamp
                );

        Duration duration =
                Duration.between(
                        created,
                        Instant.now()
                );

        long days =
                duration.toDays();

        long hours =
                duration.toHours() % 24;

        long minutes =
                duration.toMinutes() % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
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

            if (pod.get("metadata")
                    .get("name")
                    .asText()
                    .equals(podName)) {

                return pod;
            }
        }

        return null;
    }
}