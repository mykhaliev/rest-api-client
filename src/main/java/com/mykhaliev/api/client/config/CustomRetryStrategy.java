package com.mykhaliev.api.client.config;

import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Retry strategy for http client.
 */
public class CustomRetryStrategy implements ServiceUnavailableRetryStrategy {

    private int retryCount;

    private int retryIntervalMilliseconds;

    public CustomRetryStrategy(int retryCount, int retryIntervalMilliseconds) {
        this.retryCount = retryCount;
        this.retryIntervalMilliseconds = retryIntervalMilliseconds;
    }

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        int statusCode = response.getStatusLine().getStatusCode();
        return (statusCode == 401 || statusCode == 502 || statusCode == 503) && executionCount <= retryCount;
    }

    @Override
    public long getRetryInterval() {
        return retryIntervalMilliseconds;
    }
}
