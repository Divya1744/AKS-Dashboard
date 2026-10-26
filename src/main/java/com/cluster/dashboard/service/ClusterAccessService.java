
package com.cluster.dashboard.service;

import com.cluster.dashboard.config.SessionKubernetesClientProvider;
import com.cluster.dashboard.model.ClusterAccessResponse;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.VersionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterAccessService {

    private final SessionKubernetesClientProvider clientProvider;


    public ClusterAccessResponse validateClusterAccess() {
        try {
            ApiClient client =
                    clientProvider.getApiClient();

            if (client == null) {
                return new ClusterAccessResponse(
                        "ERROR",
                        "KUBERNETES_CLIENT_UNAVAILABLE",
                        "No active Kubernetes client is available.",
                        null
                );
            }

            CoreV1Api coreApi =
                    new CoreV1Api(client);

            CustomObjectsApi customApi =
                    new CustomObjectsApi(client);

            VersionApi versionApi =
                    new VersionApi(client);

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

            return new ClusterAccessResponse(
                    "SUCCESS",
                    "CLUSTER_ACCESS_GRANTED",
                    "The configured kubeconfig has the required access.",
                    clientProvider.getActiveClusterName()
            );

        } catch (ApiException e) {
            String code = e.getCode() == 401
                    ? "AUTHENTICATION_FAILED"
                    : e.getCode() == 403
                    ? "CLUSTER_ACCESS_DENIED"
                    : "KUBERNETES_API_ERROR";

            String status = (e.getCode() == 401 || e.getCode() == 403)
                    ? "RESTRICTED"
                    : "ERROR";

            return new ClusterAccessResponse(
                    status,
                    code,
                    "Unable to validate required Kubernetes access. HTTP "
                            + e.getCode() + ".",
                    null
            );
        }
    }

}
