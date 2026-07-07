package com.jsontools.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client service for fetching JSON from URLs.
 */
public class HttpService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode fetchJson(String url) throws IOException, InterruptedException {
        return fetchJson(url, null);
    }

    public JsonNode fetchJson(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readTree(response.body());
        } else {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    public HttpResponse<String> execute(String url, String method, Map<String, String> headers,
                                         Map<String, String> queryParams, String body)
            throws IOException, InterruptedException {

        String fullUrl = buildUrl(url, queryParams);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofMinutes(10));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        switch (method.toUpperCase()) {
            case "POST":
                requestBuilder.POST(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                break;
            case "PUT":
                requestBuilder.PUT(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            case "PATCH":
                requestBuilder.method("PATCH", body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                break;
            default:
                requestBuilder.GET();
                break;
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String buildUrl(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            if (!first) sb.append("&");
            sb.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }
}
