package com.jsontools.testing;

import com.jsontools.model.BatchTestResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates and computes performance statistics from batch test results.
 */
public class TestStatistics {

    private final List<BatchTestResult> results;

    public TestStatistics(List<BatchTestResult> results) {
        this.results = results;
    }

    public int getTotalRequests() {
        return results.size();
    }

    public int getSuccessCount() {
        return (int) results.stream().filter(BatchTestResult::isSuccess).count();
    }

    public int getFailureCount() {
        return getTotalRequests() - getSuccessCount();
    }

    public double getSuccessRate() {
        if (results.isEmpty()) return 0.0;
        return (double) getSuccessCount() / getTotalRequests() * 100.0;
    }

    public long getMinResponseTime() {
        return results.stream()
                .mapToLong(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .min()
                .orElse(0);
    }

    public long getMaxResponseTime() {
        return results.stream()
                .mapToLong(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .max()
                .orElse(0);
    }

    public double getAverageResponseTime() {
        return results.stream()
                .mapToLong(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .average()
                .orElse(0.0);
    }

    public double getMedianResponseTime() {
        List<Long> sorted = results.stream()
                .map(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .sorted()
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return 0.0;
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
        return sorted.get(mid);
    }

    public double getP95ResponseTime() {
        List<Long> sorted = results.stream()
                .map(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .sorted()
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return 0.0;
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.min(index, sorted.size() - 1));
    }

    public double getP99ResponseTime() {
        List<Long> sorted = results.stream()
                .map(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .sorted()
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return 0.0;
        int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.min(index, sorted.size() - 1));
    }

    public long getTotalTime() {
        return results.stream()
                .mapToLong(BatchTestResult::getResponseTimeMs)
                .filter(t -> t >= 0)
                .sum();
    }

    public double getThroughput() {
        long totalTimeSeconds = getTotalTime() / 1000;
        if (totalTimeSeconds == 0) return results.size();
        return (double) results.size() / totalTimeSeconds;
    }

    public List<BatchTestResult> getFailedResults() {
        return results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("""
                ═══════════════════════════════════════
                         TEST STATISTICS REPORT
                ═══════════════════════════════════════
                Total Requests:     %d
                Successful:         %d (%.1f%%)
                Failed:             %d
                ───────────────────────────────────────
                Response Times:
                  Min:              %d ms
                  Max:              %d ms
                  Average:          %.1f ms
                  Median:           %.1f ms
                  P95:              %.1f ms
                  P99:              %.1f ms
                ───────────────────────────────────────
                Total Time:         %d ms
                Throughput:         %.2f req/s
                ═══════════════════════════════════════
                """,
                getTotalRequests(),
                getSuccessCount(), getSuccessRate(),
                getFailureCount(),
                getMinResponseTime(),
                getMaxResponseTime(),
                getAverageResponseTime(),
                getMedianResponseTime(),
                getP95ResponseTime(),
                getP99ResponseTime(),
                getTotalTime(),
                getThroughput()
        );
    }
}
