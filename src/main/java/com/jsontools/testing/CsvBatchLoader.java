package com.jsontools.testing;

import com.jsontools.model.BatchTestRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads batch test requests from a CSV file of NIT values.
 * Each line in the CSV should contain: nit_value,check_digit
 * 
 * Generates GET requests to the configured endpoint with Basic Auth.
 */
public class CsvBatchLoader {

    private final String baseUrl;
    private final String authHeader;
    private final int expectedStatus;

    /**
     * @param baseUrl       Base URL for the endpoint (e.g., "http://v121h159:7001/extrato/v5/consulta")
     * @param username      Basic auth username
     * @param password      Basic auth password
     * @param expectedStatus Expected HTTP status code for success
     */
    public CsvBatchLoader(String baseUrl, String username, String password, int expectedStatus) {
        this.baseUrl = baseUrl;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
        this.expectedStatus = expectedStatus;
    }

    /**
     * Load NITs from a CSV file and generate batch test requests.
     * CSV format: nit_value,check_digit (one per line, no header)
     */
    public List<BatchTestRequest> loadFromCsv(File csvFile) throws IOException {
        List<BatchTestRequest> requests = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile.toPath())) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 1) continue;

                String nit = parts[0].trim();
                if (nit.isEmpty()) continue;

                BatchTestRequest request = new BatchTestRequest();
                request.setName("Extrato NIT " + nit + " (#" + lineNum + ")");
                request.setUrl(baseUrl);
                request.setMethod("GET");

                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", authHeader);
                headers.put("Accept", "application/json");
                request.setHeaders(headers);

                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("nit", nit);
                request.setQueryParams(queryParams);

                request.setBody(null);
                request.setExpectedStatus(expectedStatus);

                requests.add(request);
            }
        }

        return requests;
    }

    /**
     * Load a subset of NITs from the CSV (for quick sampling).
     * @param maxCount Maximum number of requests to generate
     */
    public List<BatchTestRequest> loadFromCsv(File csvFile, int maxCount) throws IOException {
        List<BatchTestRequest> all = loadFromCsv(csvFile);
        if (all.size() <= maxCount) return all;

        // Evenly sample from the list
        List<BatchTestRequest> sampled = new ArrayList<>();
        double step = (double) all.size() / maxCount;
        for (int i = 0; i < maxCount; i++) {
            sampled.add(all.get((int) (i * step)));
        }
        return sampled;
    }
}
