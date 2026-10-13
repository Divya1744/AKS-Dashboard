package com.cluster.dashboard.service;

import com.cluster.dashboard.model.ClusterResponse;
import com.cluster.dashboard.model.NodeSummary;
import com.cluster.dashboard.model.ResourceUsage;
import com.cluster.dashboard.util.ResourceParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final KubernetesApiService kubernetesApiService;
    private final MetricsApiService metricsApiService;


    public ClusterResponse getClusterOverview(String clusterName) {
        String kubernetesVersion =
                kubernetesApiService.getKubernetesVersion();

        V1NodeList nodes =
                kubernetesApiService.getNodes();

        V1NamespaceList namespaces =
                kubernetesApiService.getNamespaces();

        V1PodList pods =
                kubernetesApiService.getAllPods();

        JsonNode nodeMetrics =
                metricsApiService.getNodeMetrics();


        // Inside ClusterService.java -> pass pods to createNodeSummaries:
        List<NodeSummary> nodeSummaries = createNodeSummaries(nodes, nodeMetrics, pods);

        ResourceTotals totals =
                calculateResourceTotals(nodes, pods, nodeMetrics);

        ResourceUsage cpu =
                new ResourceUsage(
                        ResourceParser.cpuToString(
                                totals.cpuUsage()),
                        ResourceParser.cpuToString(
                                totals.cpuRequest()),
                        ResourceParser.cpuToString(
                                totals.cpuLimit()),
                        ResourceParser.utilization(
                                totals.cpuUsage(),
                                totals.cpuCapacity())
                );

        ResourceUsage memory =
                new ResourceUsage(
                        ResourceParser.memoryToGi(
                                totals.memoryUsage()),
                        ResourceParser.memoryToGi(
                                totals.memoryRequest()),
                        ResourceParser.memoryToGi(
                                totals.memoryLimit()),
                        ResourceParser.utilization(
                                totals.memoryUsage(),
                                totals.memoryCapacity())
                );

        int running = 0;
        int pending = 0;
        int failed = 0;

        if (pods != null && pods.getItems() != null) {

            for (V1Pod pod : pods.getItems()) {

                if (pod.getStatus() == null) {
                    continue;
                }

                String status =
                        pod.getStatus().getPhase();

                if ("Running".equals(status)) {
                    running++;
                } else if ("Pending".equals(status)) {
                    pending++;
                } else if ("Failed".equals(status)) {
                    failed++;
                }
            }
        }

        int totalPods =
                pods == null || pods.getItems() == null
                        ? 0
                        : pods.getItems().size();

        int totalNamespaces =
                namespaces == null ||
                        namespaces.getItems() == null
                        ? 0
                        : namespaces.getItems().size();

        int totalNodes =
                nodes == null ||
                        nodes.getItems() == null
                        ? 0
                        : nodes.getItems().size();

        int readyNodes = 0;

        for (NodeSummary node : nodeSummaries) {

            if ("Ready".equals(node.status())) {
                readyNodes++;
            }
        }

        int notReadyNodes =
                totalNodes - readyNodes;

        return new ClusterResponse(
                clusterName,
                kubernetesVersion,
                totalNodes,
                readyNodes,
                notReadyNodes,
                totalNamespaces,
                totalPods,
                running,
                pending,
                failed,
                nodeSummaries,
                cpu,
                memory
        );
    }

    private List<NodeSummary> createNodeSummaries(
            V1NodeList nodes,
            JsonNode metrics,
            V1PodList pods) {

        List<NodeSummary> result =
                new ArrayList<>();

        if (nodes == null ||
                nodes.getItems() == null) {
            return result;
        }

        for (V1Node node : nodes.getItems()) {

            String name =
                    node.getMetadata().getName();

            String status =
                    getNodeStatus(node);

            // Count pods assigned to this node
            int podCount = 0;
            if (pods != null && pods.getItems() != null) {
                for (V1Pod pod : pods.getItems()) {
                    if (pod.getSpec() != null && name.equals(pod.getSpec().getNodeName())) {
                        podCount++;
                    }
                }
            }

            Map<String, Quantity> capacity =
                    node.getStatus().getCapacity();

            Map<String, Quantity> allocatable =
                    node.getStatus().getAllocatable();

            long cpuCapacity =
                    getCpu(capacity, "cpu");

            long cpuAllocatable =
                    getCpu(allocatable, "cpu");

            long memoryCapacity =
                    getMemory(capacity, "memory");

            long memoryAllocatable =
                    getMemory(allocatable, "memory");

            JsonNode nodeMetric =
                    findNodeMetric(metrics, name);

            long cpuUsage =
                    getCpuUsage(nodeMetric);

            long memoryUsage =
                    getMemoryUsage(nodeMetric);

            result.add(
                    new NodeSummary(
                            name,
                            status,
                            podCount,
                            ResourceParser.cpuToString(cpuUsage),
                            ResourceParser.cpuToString(cpuCapacity),
                            ResourceParser.cpuToString(cpuAllocatable),
                            ResourceParser.memoryToGi(memoryUsage),
                            ResourceParser.memoryToGi(memoryCapacity),
                            ResourceParser.memoryToGi(memoryAllocatable)
                    )
            );
        }

        return result;
    }

    private String getNodeStatus(V1Node node) {

        if (node.getStatus() == null ||
                node.getStatus().getConditions() == null) {
            return "Unknown";
        }

        for (var condition :
                node.getStatus().getConditions()) {

            if ("Ready".equals(condition.getType())) {

                if ("True".equals(condition.getStatus())) {
                    return "Ready";
                }

                return "NotReady";
            }
        }

        return "Unknown";
    }

    private long getCpu(
            Map<String, Quantity> resources,
            String key) {

        if (resources == null) {
            return 0;
        }

        return ResourceParser.parseCpu(
                resources.get(key));
    }

    private long getMemory(
            Map<String, Quantity> resources,
            String key) {

        if (resources == null) {
            return 0;
        }

        return ResourceParser.parseMemory(
                resources.get(key));
    }

    private JsonNode findNodeMetric(
            JsonNode metrics,
            String nodeName) {

        if (metrics == null ||
                !metrics.has("items")) {
            return null;
        }

        for (JsonNode node :
                metrics.get("items")) {

            if (node.has("metadata") &&
                    node.get("metadata").has("name") &&
                    node.get("metadata")
                            .get("name")
                            .asText()
                            .equals(nodeName)) {

                return node;
            }
        }

        return null;
    }

    private long getCpuUsage(
            JsonNode nodeMetric) {

        if (nodeMetric == null ||
                !nodeMetric.has("usage") ||
                !nodeMetric.get("usage")
                        .has("cpu")) {
            return 0;
        }

        return ResourceParser.parseCpuToMilli(
                nodeMetric.get("usage")
                        .get("cpu")
                        .asText());
    }

    private long getMemoryUsage(
            JsonNode nodeMetric) {

        if (nodeMetric == null ||
                !nodeMetric.has("usage") ||
                !nodeMetric.get("usage")
                        .has("memory")) {
            return 0;
        }

        return ResourceParser.parseMemoryToKi(
                nodeMetric.get("usage")
                        .get("memory")
                        .asText());
    }

    private ResourceTotals calculateResourceTotals(
            V1NodeList nodes,
            V1PodList pods,
            JsonNode nodeMetrics) {

        long cpuCapacity = 0;
        long cpuRequest = 0;
        long cpuLimit = 0;
        long memoryCapacity = 0;
        long memoryRequest = 0;
        long memoryLimit = 0;
        long cpuUsage = 0;
        long memoryUsage = 0;

        if (nodes != null &&
                nodes.getItems() != null) {

            for (V1Node node :
                    nodes.getItems()) {

                Map<String, Quantity> capacity =
                        node.getStatus().getCapacity();

                cpuCapacity +=
                        getCpu(capacity, "cpu");

                memoryCapacity +=
                        getMemory(capacity, "memory");
            }
        }

        if (pods != null &&
                pods.getItems() != null) {

            for (V1Pod pod :
                    pods.getItems()) {

                if (pod.getSpec() == null ||
                        pod.getSpec().getContainers() == null) {
                    continue;
                }

                for (var container :
                        pod.getSpec().getContainers()) {

                    if (container.getResources() == null) {
                        continue;
                    }

                    Map<String, Quantity> requests =
                            container.getResources()
                                    .getRequests();

                    Map<String, Quantity> limits =
                            container.getResources()
                                    .getLimits();

                    if (requests != null) {

                        cpuRequest +=
                                getCpu(requests, "cpu");

                        memoryRequest +=
                                getMemory(requests, "memory");
                    }

                    if (limits != null) {

                        cpuLimit +=
                                getCpu(limits, "cpu");

                        memoryLimit +=
                                getMemory(limits, "memory");
                    }
                }
            }
        }

        if (nodeMetrics != null &&
                nodeMetrics.has("items")) {

            for (JsonNode node :
                    nodeMetrics.get("items")) {

                if (!node.has("usage")) {
                    continue;
                }

                JsonNode usage =
                        node.get("usage");

                if (usage.has("cpu")) {
                    cpuUsage +=
                            ResourceParser.parseCpuToMilli(
                                    usage.get("cpu").asText());
                }

                if (usage.has("memory")) {
                    memoryUsage +=
                            ResourceParser.parseMemoryToKi(
                                    usage.get("memory").asText());
                }
            }
        }

        return new ResourceTotals(
                cpuCapacity,
                cpuRequest,
                cpuLimit,
                memoryCapacity,
                memoryRequest,
                memoryLimit,
                cpuUsage,
                memoryUsage
        );
    }

    private record ResourceTotals(
            long cpuCapacity,
            long cpuRequest,
            long cpuLimit,
            long memoryCapacity,
            long memoryRequest,
            long memoryLimit,
            long cpuUsage,
            long memoryUsage
    ) {
    }
}