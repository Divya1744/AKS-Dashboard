package com.cluster.dashboard.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

@Configuration
public class KubernetesClientConfig {

    private static final String KUBECONFIG = "//wsl.localhost/WindchillVM/home/wncuser/.kube/kubeconfig-plm-rd-us 2";

    private static final String CONTEXT =
            "octopus";

    @Bean
    public ApiClient kubernetesApiClient()
            throws IOException {

        KubeConfig kubeConfig =
                KubeConfig.loadKubeConfig(
                        new FileReader(KUBECONFIG)
                );

        kubeConfig.setContext(CONTEXT);

        return ClientBuilder
                .kubeconfig(kubeConfig)
                .build();
    }
}