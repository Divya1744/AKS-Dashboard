package com.cluster.dashboard.controller;

import com.cluster.dashboard.model.NamespaceResponse;
import com.cluster.dashboard.model.NamespaceSummary;
import com.cluster.dashboard.model.PodDetails;
import com.cluster.dashboard.service.KubernetesApiService;
import com.cluster.dashboard.service.MetricsApiService;
import com.cluster.dashboard.service.NamespaceService;
import com.cluster.dashboard.service.PodService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final NamespaceService namespaceService;
    private final KubernetesApiService kubernetesApiService;
    private final MetricsApiService metricsApiService;
    private final PodService podService;

    @GetMapping("/clusters/plm-rd-us/namespaces/{namespace}")
    public NamespaceResponse getNamespace(@PathVariable String namespace) {

        return namespaceService.getNamespaceOverview(namespace);
    }

    @GetMapping("/clusters/plm-rd-us/namespaces")
    public List<NamespaceSummary> getNamespaces() {

        return namespaceService.getNamespaces();
    }

    /* @GetMapping("/clusters/plm-rd-us/namespaces/{namespace}/pods/{podName}")
    public PodDetails getPod(@PathVariable String namespace, @PathVariable String podName) {

        JsonNode pod = kubernetesApiService.getPod(namespace,podName);

        JsonNode metrics = metricsApiService.getPodMetrics(namespace,podName);

        return podService.createPodDetails(
                pod, metrics
        );
    }**/
}