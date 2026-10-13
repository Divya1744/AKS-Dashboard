package com.cluster.dashboard.controller;

import com.cluster.dashboard.model.ClusterResponse;
import com.cluster.dashboard.model.NamespaceResponse;
import com.cluster.dashboard.model.NamespaceSummary;
import com.cluster.dashboard.service.KubernetesApiService;
import com.cluster.dashboard.service.NamespaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cluster.dashboard.service.ClusterService;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

public class DashboardController {

    private final NamespaceService namespaceService;
    private final ClusterService clusterService;
    private final KubernetesApiService kubernetesApiService;

    @GetMapping(
            "/clusters/plm-rd-us/namespaces"
    )
    public List<NamespaceSummary> getNamespaces() {

        return namespaceService.getNamespaces();
    }

    @GetMapping(
            "/clusters/plm-rd-us/namespaces/{namespace}"
    )
    public NamespaceResponse getNamespace(
            @PathVariable String namespace) {

        return namespaceService
                .getNamespaceOverview(namespace);
    }

    @GetMapping("/clusters/plm-rd-us")
    public ClusterResponse getClusterOverview() {

        return clusterService.getClusterOverview("plm-rd-us");
    }

    @GetMapping(
            "/clusters/{clusterName}/namespaces/{namespace}/pods/{podName}/containers/{containerName}/logs"
    )
    public String getContainerLogs(
            @PathVariable String clusterName,
            @PathVariable String namespace,
            @PathVariable String podName,
            @PathVariable String containerName,
            @RequestParam(defaultValue = "500") Integer tailLines) {

        return kubernetesApiService.getContainerLogs(
                namespace,
                podName,
                containerName,
                tailLines
        );
    }
}