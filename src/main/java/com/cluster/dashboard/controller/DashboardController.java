package com.cluster.dashboard.controller;

import com.cluster.dashboard.model.NamespaceResponse;
import com.cluster.dashboard.model.NamespaceSummary;
import com.cluster.dashboard.service.NamespaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final NamespaceService namespaceService;

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
}