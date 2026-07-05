package com.jsontools.model;

/**
 * Holds the result of a single batch test execution.
 */
public class BatchTestResult {

    private final BatchTestRequest request;
    private final int statusCode;
    private final String responseBody;
    private final long responseTimeMs;
    private final boolean success;
    private final String errorMessage;

    private BatchTestResult(Builder builder) {
        this.request = builder.request;
        this.statusCode = builder.statusCode;
        this.responseBody = builder.responseBody;
        this.responseTimeMs = builder.responseTimeMs;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    public BatchTestRequest getRequest() {
        return request;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRequestName() {
        return request.getName();
    }

    public String getRequestMethod() {
        return request.getMethod();
    }

    public String getRequestUrl() {
        return request.getUrl();
    }

    public static class Builder {
        private BatchTestRequest request;
        private int statusCode;
        private String responseBody;
        private long responseTimeMs;
        private boolean success;
        private String errorMessage;

        public Builder request(BatchTestRequest request) {
            this.request = request;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public BatchTestResult build() {
            return new BatchTestResult(this);
        }
    }
}
