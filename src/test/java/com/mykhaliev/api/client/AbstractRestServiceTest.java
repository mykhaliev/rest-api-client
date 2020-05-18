package com.mykhaliev.api.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.mykhaliev.api.client.config.RestApiClientConfig;
import com.mykhaliev.api.client.model.api.Response;
import org.junit.After;
import org.junit.Rule;


public class AbstractRestServiceTest {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(
                    DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    protected RestApiClientConfig testConfig = RestApiClientConfig
            .builder()
            .apiRootUrl("http://localhost:8089")
            .username("username")
            .password("password")
            .build();

    protected PetstoreRestClient client = new PetstoreRestClient(testConfig);

    @After
    public void afterTest() {
        wireMockRule.resetMappings();
    }

    public static <T> Response<T> createResponse(T result) {
        Response<T> response = new Response<>();
        response.setApiVersion(1);
        response.setResult(result);
        return response;
    }
}
