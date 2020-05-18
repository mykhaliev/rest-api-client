package com.mykhaliev.api.client.config;

import lombok.Builder;
import lombok.Data;

/**
 * Rest client config.
 */
@Data
@Builder
public class RestApiClientConfig {

    private String apiRootUrl;
    private String username;
    private String password;
    private String proxyHost;
    private int proxyPort = 8080;
    private String proxyUser;
    private String proxyPassword;
    private int poolMaxPerRoute = 10;
    private int poolMaxTotal = 20;
    private int connectionRequestTimeout = 5000;
    private int connectTimeout = 5000;
    private int socketTimeout = 5000;
    private boolean verifySsl;
    private String caBundle;
    private int retryCount = 1;
    private int retryIntervalMilliseconds = 1000;

}
