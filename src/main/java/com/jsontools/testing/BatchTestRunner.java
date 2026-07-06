package com.jsontools.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsontools.model.BatchTestRequest;
import com.jsontools.model.BatchTestResult;
import com.jsontools.service.HttpService;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Executes batch HTTP tests from a configuration file and collects performance statistics.
 */
public class BatchTestRunner {

    private final HttpService httpService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public BatchTestRunner() {
        this.httpService = new HttpService();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors() * 2, 10)
        );
    }

    /**
     * Load batch test requests from a JSON file.
     * Expected format:
     * [
     *   {
     *     "name": "Get Users",
     *     "url": "https://api.example.com/users",
     *     "method": "GET",
     *     "headers": {"Authorization": "Bearer token"},
     *     "queryParams": {"page": "1", "limit": "10"},
     *     "body": null,
     *     "expectedStatus": 200
     *   }
     * ]
     */
    public List<BatchTestRequest> loadRequests(File file) throws IOException {
        return objectMapper.readValue(file, new TypeReference<List<BatchTestRequest>>() {});
    }

    /**
     * Execute all tests sequentially and report results.
     */
    public List<BatchTestResult> runSequential(List<BatchTestRequest> requests,
                                                Consumer<BatchTestResult> onResult) {
        List<BatchTestResult> results = new ArrayList<>();
        for (BatchTestRequest request : requests) {
            BatchTestResult result = executeSingle(request);
            results.add(result);
            if (onResult != null) {
                onResult.accept(result);
            }
        }
        return results;
    }

    /**
     * Execute all tests in parallel and report results.
     */
    public List<BatchTestResult> runParallel(List<BatchTestRequest> requests,
                                              Consumer<BatchTestResult> onResult) {
        List<Future<BatchTestResult>> futures = new ArrayList<>();

        for (BatchTestRequest request : requests) {
            futures.add(executor.submit(() -> executeSingle(request)));
        }

        List<BatchTestResult> results = new ArrayList<>();
        for (Future<BatchTestResult> future : futures) {
            try {
                BatchTestResult result = future.get(10, TimeUnit.MINUTES);
                results.add(result);
                if (onResult != null) {
                    onResult.accept(result);
                }
            } catch (Exception e) {
                // Create a failure result for timeout/interruption
                results.add(new BatchTestResult.Builder()
                        .request(requests.get(results.size()))
                        .statusCode(-1)
                        .responseTimeMs(-1)
                        .success(false)
                        .errorMessage("Execution error: " + e.getMessage())
                        .build());
            }
        }
        return results;
    }

    private BatchTestResult executeSingle(BatchTestRequest request) {
        long startTime = System.nanoTime();
        try {
            HttpResponse<String> response = httpService.execute(
                    request.getUrl(),
                    request.getMethod(),
                    request.getHeaders(),
                    request.getQueryParams(),
                    request.getBody()
            );

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            boolean success = response.statusCode() == request.getExpectedStatus();

            return new BatchTestResult.Builder()
                    .request(request)
                    .statusCode(response.statusCode())
                    .responseBody(response.body())
                    .responseTimeMs(elapsed)
                    .success(success)
                    .errorMessage(success ? null :
                            "Expected status " + request.getExpectedStatus() +
                                    " but got " + response.statusCode())
                    .build();

        } catch (Exception e) {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            return new BatchTestResult.Builder()
                    .request(request)
                    .statusCode(-1)
                    .responseTimeMs(elapsed)
                    .success(false)
                    .errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
