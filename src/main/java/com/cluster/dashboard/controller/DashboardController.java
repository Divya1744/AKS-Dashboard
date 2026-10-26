package com.cluster.dashboard.controller;

import com.cluster.dashboard.config.SessionKubernetesClientProvider;
import com.cluster.dashboard.model.*;
import com.cluster.dashboard.service.ClusterAccessService;
import com.cluster.dashboard.service.KubernetesApiService;
import com.cluster.dashboard.service.NamespaceService;
import io.kubernetes.client.openapi.ApiException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cluster.dashboard.service.ClusterService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

public class DashboardController {

    private final NamespaceService namespaceService;
    private final ClusterService clusterService;
    private final KubernetesApiService kubernetesApiService;
    private final ClusterAccessService clusterAccessService;
    private final SessionKubernetesClientProvider sessionKubernetesClientProvider;




    @GetMapping("/clusters/{clusterName}")
    public ClusterResponse getClusterOverview(
            @PathVariable String clusterName) {

        requireActiveCluster(clusterName);
        return clusterService.getClusterOverview(clusterName);
    }

    @GetMapping("/clusters/{clusterName}/namespaces")
    public List<NamespaceSummary> getNamespaces(
            @PathVariable String clusterName) {

        requireActiveCluster(clusterName);
        return namespaceService.getNamespaces();
    }

    @GetMapping("/clusters/{clusterName}/namespaces/{namespace}")
    public NamespaceResponse getNamespace(
            @PathVariable String clusterName,
            @PathVariable String namespace) {

        requireActiveCluster(clusterName);
        return namespaceService.getNamespaceOverview(namespace);
    }

    private void requireActiveCluster(String requestedClusterName) {
        String activeClusterName =
                sessionKubernetesClientProvider.getActiveClusterName();

        if (activeClusterName == null
                || !activeClusterName.equals(requestedClusterName)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cluster not found in this session."
            );
        }
    }




    @GetMapping("/clusters/{clusterName}/namespaces/{namespace}/pods/{podName}/containers/{containerName}/logs")
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

    @GetMapping("/clusters/access")
    public ResponseEntity<ClusterAccessResponse> validateClusterAccess() {
        ClusterAccessResponse response =
                clusterAccessService.validateClusterAccess();

        HttpStatus status = switch (response.status()) {
            case "SUCCESS" -> HttpStatus.OK;
            case "RESTRICTED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return ResponseEntity.status(status).body(response);
    }




    @PostMapping(
            value = "/kubeconfig/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<KubeconfigUploadResponse> uploadKubeconfig(
            @RequestParam("file") MultipartFile file) {

        try {
            sessionKubernetesClientProvider.useUploadedKubeconfig(file);

            return ResponseEntity.ok(new KubeconfigUploadResponse(
                    "SUCCESS",
                    "KUBECONFIG_UPLOADED",
                    "Kubeconfig validated and activated for this session.",
                    sessionKubernetesClientProvider.getActiveClusterName()
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    new KubeconfigUploadResponse(
                            "ERROR",
                            "INVALID_KUBECONFIG",
                            ex.getMessage(),
                            null
                    )
            );

        } catch (ApiException ex) {
            HttpStatus status = ex.getCode() == 401 || ex.getCode() == 403
                    ? HttpStatus.FORBIDDEN
                    : HttpStatus.BAD_GATEWAY;

            return ResponseEntity.status(status).body(
                    new KubeconfigUploadResponse(
                            "RESTRICTED",
                            ex.getCode() == 401
                                    ? "AUTHENTICATION_FAILED"
                                    : ex.getCode() == 403
                                    ? "CLUSTER_ACCESS_DENIED"
                                    : "KUBERNETES_API_ERROR",
                            "The kubeconfig could not access all required resources.",
                            null
                    )
            );

        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(
                    new KubeconfigUploadResponse(
                            "ERROR",
                            "KUBECONFIG_READ_FAILED",
                            "Unable to read the uploaded kubeconfig file.",
                            null
                    )
            );
        }
    }


    @GetMapping("/kubeconfig/session-check")
    public Map<String, String> sessionCheck(
            HttpSession session) {

        return Map.of(
                "sessionId", session.getId(),
                "message", "Request is associated with this session"
        );
    }

    @GetMapping("/kubeconfig/active-context")
    public Map<String, String> getActiveContext() {
        return Map.of(
                "activeContext",
                sessionKubernetesClientProvider.getActiveContextName()
        );
    }

    @PostMapping("/kubeconfig/reset")
    public ResponseEntity<Void> resetSession(jakarta.servlet.http.HttpSession session) {
        if (session != null) {
            session.invalidate(); // Destroys the mutated session
        }
        return ResponseEntity.ok().build();
    }


}