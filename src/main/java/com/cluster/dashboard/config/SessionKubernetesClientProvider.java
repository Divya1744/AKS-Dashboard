
package com.cluster.dashboard.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
@Scope(
        value = WebApplicationContext.SCOPE_SESSION,
        proxyMode = ScopedProxyMode.TARGET_CLASS
)
public class SessionKubernetesClientProvider {

    @Value("${kubeconfig.path}")
    private String defaultKubeconfigPath;

    private ApiClient apiClient;
    private String activeContextName;
    private String activeClusterName;

    @PostConstruct
    public synchronized void initialize() throws IOException {
        activateKubeconfig(defaultKubeconfigPath);
    }

    public synchronized ApiClient getApiClient() {
        return apiClient;
    }

    public synchronized String getActiveContextName() {
        return activeContextName;
    }

    public synchronized String getActiveClusterName() {
        return activeClusterName;
    }

    public synchronized void useUploadedKubeconfig(
            MultipartFile file
    ) throws IOException, ApiException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "The uploaded kubeconfig file is empty."
            );
        }

        KubeConfig kubeConfig;

        try (Reader reader = new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8
        )) {
            kubeConfig = KubeConfig.loadKubeConfig(reader);
        }

        activateKubeconfig(kubeConfig);
    }

    private void activateKubeconfig(String path) throws IOException {
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(Path.of(path)),
                StandardCharsets.UTF_8
        )) {
            KubeConfig kubeConfig = KubeConfig.loadKubeConfig(reader);
            activateKubeconfig(kubeConfig);
        } catch (ApiException e) {
            throw new IOException(
                    "Unable to validate the configured kubeconfig.",
                    e
            );
        }
    }

    private synchronized void activateKubeconfig(
            KubeConfig kubeConfig
    ) throws ApiException, IOException {

        String candidateContextName = kubeConfig.getCurrentContext();
        String candidateClusterName = resolveClusterName(kubeConfig);

        ApiClient candidateClient = ClientBuilder
                .kubeconfig(kubeConfig)
                .build();

        // Do not replace the active session client until validation passes.
        validateRequiredAccess(candidateClient);

        this.apiClient = candidateClient;
        this.activeContextName = candidateContextName;
        this.activeClusterName = candidateClusterName;
    }

    private String resolveClusterName(KubeConfig kubeConfig) {
        String currentContext = kubeConfig.getCurrentContext();

        if (currentContext == null || currentContext.isBlank()) {
            throw new IllegalArgumentException(
                    "The kubeconfig has no current-context configured."
            );
        }

        List<?> contexts = kubeConfig.getContexts();

        if (contexts != null) {
            for (Object item : contexts) {
                if (!(item instanceof Map<?, ?> entry)) {
                    continue;
                }

                if (!currentContext.equals(entry.get("name"))) {
                    continue;
                }

                Object contextObject = entry.get("context");

                if (!(contextObject instanceof Map<?, ?> context)) {
                    break;
                }

                Object cluster = context.get("cluster");

                if (cluster instanceof String clusterName
                        && !clusterName.isBlank()) {
                    return clusterName;
                }

                break;
            }
        }

        throw new IllegalArgumentException(
                "Could not resolve the cluster referenced by current-context: "
                        + currentContext
        );
    }

    private void validateRequiredAccess(ApiClient candidate)
            throws ApiException {

        CoreV1Api coreApi = new CoreV1Api(candidate);
        CustomObjectsApi customApi = new CustomObjectsApi(candidate);
        VersionApi versionApi = new VersionApi(candidate);

        coreApi.listNode().execute();
        coreApi.listNamespace().execute();
        coreApi.listPodForAllNamespaces().execute();
        coreApi.listServiceForAllNamespaces().execute();

        versionApi.getCode().execute();

        customApi.listClusterCustomObject(
                "metrics.k8s.io", "v1beta1", "nodes"
        ).execute();

        customApi.listClusterCustomObject(
                "metrics.k8s.io", "v1beta1", "pods"
        ).execute();
    }


}
