package com.mykhaliev.api.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykhaliev.api.client.config.CustomProxyRoutePlanner;
import com.mykhaliev.api.client.config.CustomRetryStrategy;
import com.mykhaliev.api.client.config.RestApiClientConfig;
import com.mykhaliev.api.client.exception.RestApiClientException;
import com.mykhaliev.api.client.model.api.RequestMethod;
import com.mykhaliev.api.client.model.api.Response;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Example of rest client implementation.
 */
public class RestApiClient implements RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final CloseableHttpClient client;

    private final String baseUrl;

    private final HttpClientContext clientContext;

    public RestApiClient(RestApiClientConfig config) {
        if (config == null) {
            throw new RestApiClientException("Api client config must be provided.");
        }
        if (config.getApiRootUrl() == null) {
            throw new RestApiClientException("Api root url must be specified.");
        }
        if (config.getUsername() == null) {
            throw new RestApiClientException("Username must be specified.");
        }
        baseUrl = sanitizeUrl(config.getApiRootUrl());

        //auth setup
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthScope authScope = new AuthScope(AuthScope.ANY);
        Credentials credentials = new UsernamePasswordCredentials(config.getUsername(), config.getPassword());
        credentialsProvider.setCredentials(authScope, credentials);
        AuthCache authCache = new BasicAuthCache();
        HttpHost authHost;
        try {
            URL baseUrlParsed = new URL(baseUrl);
            authHost = new HttpHost(baseUrlParsed.getHost(), baseUrlParsed.getPort(), baseUrlParsed.getProtocol());
        } catch (MalformedURLException e) {
            throw new RestApiClientException("Error finding protocol in api root url, should start with http or https");
        }
        authCache.put(authHost, new BasicScheme());
        //client builder
        HttpClientBuilder builder = HttpClientBuilder.create();

        //proxy setup
        if (config.getProxyHost() != null) {
            URL proxyUrl;
            try {
                proxyUrl = new URL(config.getProxyHost());
            } catch (MalformedURLException e) {
                throw new RestApiClientException("Error finding protocol in proxy url, should start with http or https");
            }

            HttpHost proxyHost = new HttpHost(proxyUrl.getHost(), config.getProxyPort());

            builder.setRoutePlanner(new CustomProxyRoutePlanner(proxyHost));
            builder.setProxy(proxyHost);

            if (config.getProxyUser() != null) {
                Credentials proxyCredentials = new UsernamePasswordCredentials(config.getProxyUser(),
                        config.getProxyPassword());
                AuthScope proxyScope = new AuthScope(proxyUrl.getHost(), config.getProxyPort());
                credentialsProvider.setCredentials(proxyScope, proxyCredentials);
                builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            }
        }

        clientContext = HttpClientContext.create();
        clientContext.setCredentialsProvider(credentialsProvider);
        clientContext.setAuthCache(authCache);

        //ssl factory
        SSLConnectionSocketFactory sslFactory;
        try {
            sslFactory = buildSslFactory(config.isVerifySsl(), config.getCaBundle());
        } catch (Exception e) {
            throw new RestApiClientException(e);
        }

        //connection managers
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslFactory).build());

        if (config.getPoolMaxPerRoute() > 0) {
            manager.setDefaultMaxPerRoute(config.getPoolMaxPerRoute());
        }
        if (config.getPoolMaxTotal() > 0) {
            manager.setMaxTotal(config.getPoolMaxTotal());
        }

        //request config
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        if (config.getConnectTimeout() > 0) {
            configBuilder.setConnectTimeout(config.getConnectTimeout());
        }
        if (config.getSocketTimeout() > 0) {
            configBuilder.setSocketTimeout(config.getSocketTimeout());
        }
        if (config.getConnectionRequestTimeout() > 0) {
            configBuilder.setConnectionRequestTimeout(config.getConnectionRequestTimeout());
        }

        builder.setDefaultRequestConfig(configBuilder.build());

        //retry strategy
        if (config.getRetryCount() > 0) {
            builder.setServiceUnavailableRetryStrategy(
                    new CustomRetryStrategy(config.getRetryCount(), config.getRetryIntervalMilliseconds()));
        }
        client = builder.build();
    }


    @Override
    public <T> T callWithJson(RequestMethod method, String path, TypeReference<T> responseType, Object payload) {
        //add base url
        path = baseUrl + path;

        HttpUriRequest request = getHttpRequest(method, path, getJsonPayload(payload));
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        return executeAndParseResponse(path, request, responseType);
    }

    @Override
    public <T> T callWithMultipart(RequestMethod method, String path, TypeReference<T> responseType, File payload) {
        if (payload == null) {
            throw new RestApiClientException("File cannot be null.");
        }
        //add base url
        path = baseUrl + path;
        HttpUriRequest request = getHttpRequest(method, path, getMultipartPayload(payload));

        request.setHeader("Accept", "application/json");
        return executeAndParseResponse(path, request, responseType);
    }

    @Override
    public void downloadFile(String path, String destinationPath) {
        try {
            //add base url
            path = baseUrl + path;
            HttpGet get = new HttpGet(path);
            File file = new File(destinationPath);
            client.execute(get, new FileDownloadResponseHandler(file), clientContext);
        } catch (Exception e) {
            throw new RestApiClientException(e);
        }
    }

    /**
     * Http request handler.
     *
     * @param path         url path
     * @param request      Apache http request
     * @param responseType jackson type to deserialize response.
     * @return parsed response.
     */
    private <T> T executeAndParseResponse(String path, HttpUriRequest request, TypeReference<T> responseType) {
        HttpResponse response;
        try {
            response = client.execute(request, clientContext);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RestApiClientException("Failed to execute http request.");
        }
        int statusCode = response.getStatusLine().getStatusCode();
        //handle error response
        if (statusCode < 200 || statusCode >= 300) {
            Response errorResponse;
            try {
                errorResponse = fromJson(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8),
                        new TypeReference<Response>() {
                        });
                throw new RestApiClientException(path, statusCode, errorResponse);
            } catch (IOException e) {
                throw new RestApiClientException(e);
            }
        }
        //handle successful response
        try {
            if (responseType == null) {
                return null;
            }
            return fromJson(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), responseType);
        } catch (IOException e) {
            throw new RestApiClientException(e);
        }
    }

    private static HttpUriRequest getHttpRequest(RequestMethod method, String path, HttpEntity payload) {
        switch (method) {
            case GET:
                return new HttpGet(path);
            case HEAD:
                return new HttpHead(path);
            case POST:
                HttpPost httpPost = new HttpPost(path);
                httpPost.setEntity(payload);
                return httpPost;
            case PUT:
                HttpPut httpPut = new HttpPut(path);
                httpPut.setEntity(payload);
                return httpPut;
            case PATCH:
                HttpPatch httpPatch = new HttpPatch(path);
                httpPatch.setEntity(payload);
                return httpPatch;
            case DELETE:
                return new HttpDelete(path);
            case OPTIONS:
                return new HttpOptions(path);
            case TRACE:
                return new HttpTrace(path);
            default:
                throw new IllegalArgumentException("Invalid http method");
        }
    }

    private static HttpEntity getJsonPayload(Object payload) {
        if (payload != null) {
            try {
                return new StringEntity(MAPPER.writeValueAsString(payload), ContentType.APPLICATION_JSON);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to create payload", e);
                throw new RestApiClientException(e);
            }
        }
        return null;
    }

    private static HttpEntity getMultipartPayload(File payload) {
        if (payload != null) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("file", new FileBody(payload, ContentType.create("multipart/form-data")));
            return builder.build();
        }
        return null;
    }

    private static <T> T fromJson(String json, TypeReference<T> responseType) {
        try {
            return MAPPER.readValue(json, responseType);
        } catch (Exception e) {
            throw new RestApiClientException(e);
        }
    }

    private static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    static class FileDownloadResponseHandler implements ResponseHandler<File> {

        private final File target;

        public FileDownloadResponseHandler(File target) {
            this.target = target;
        }

        @Override
        public File handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            InputStream source = response.getEntity().getContent();
            FileUtils.copyInputStreamToFile(source, this.target);
            return this.target;
        }

    }

    /**
     * Ssl factory builder.
     */
    public static SSLConnectionSocketFactory buildSslFactory(boolean verifySsl, String requestCaBundle)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        if (!verifySsl) {
            LOGGER.info("Client configured with trust all strategy");
            TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        } else if (requestCaBundle == null) {
            LOGGER.info("Client configured with default truststore strategy");
            return new SSLConnectionSocketFactory(SSLContexts.createDefault());
        } else {
            LOGGER.info("Client configured with custom certificate");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert;
            try (InputStream in = new FileInputStream(requestCaBundle)) {
                caCert = (X509Certificate) cf.generateCertificate(in);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("caCert", caCert);
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        }
    }

}
