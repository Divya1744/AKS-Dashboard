package com.cluster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class KubernetesApiService {

    private static final String BASE_URL = "http://127.0.0.1:8001";

    //private static final String NAMESPACE = "rd-us-dsivasubramanian";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

    public JsonNode getPods(String namespace) {

        String url =
                BASE_URL +
                        "/api/v1/namespaces/" +
                        namespace +
                        "/pods";

        return get(url);
    }

    public JsonNode getPod(String namespace, String podName) {

        String url =
                BASE_URL +
                        "/api/v1/namespaces/" +
                        namespace +
                        "/pods/" +
                        podName;

        return get(url);
    }

    private JsonNode get(String url) {              //communicates with the k8s api

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Kubernetes API returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            return objectMapper.readTree(response.body());

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to contact Kubernetes API",
                    e
            );
        }
    }

    public JsonNode getNamespaces() {

        String url =
                BASE_URL +
                        "/api/v1/namespaces";

        return get(url);
    }
}