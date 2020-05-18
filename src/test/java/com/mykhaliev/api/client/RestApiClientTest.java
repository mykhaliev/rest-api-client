package com.mykhaliev.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mykhaliev.api.client.config.RestApiClientConfig;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class RestApiClientTest extends AbstractRestServiceTest {

    @Test
    public void retryIgnoreTest() throws JsonProcessingException {
        RestApiClientConfig testConfig = RestApiClientConfig
                .builder()
                .apiRootUrl("http://localhost:8089")
                .username("username")
                .password("password")
                .retryCount(5)
                .retryIntervalMilliseconds(1)
                .build();

        PetstoreRestClient client = new PetstoreRestClient(testConfig);

        stubFor(get(urlEqualTo("/pets/1"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(badRequest().withBody(MAPPER.writeValueAsString(createResponse("bad request")))));
        try {
            client.getPetById(1);
        } catch (Exception e) {
            verify(exactly(1), getRequestedFor(urlEqualTo("/pets/1")));
        }
    }

    @Test
    public void retryTest() throws JsonProcessingException {
        RestApiClientConfig testConfig = RestApiClientConfig
                .builder()
                .apiRootUrl("http://localhost:8089")
                .username("username")
                .password("password")
                .retryCount(5)
                .retryIntervalMilliseconds(1)
                .build();

        PetstoreRestClient client = new PetstoreRestClient(testConfig);

        stubFor(get(urlEqualTo("/pets/1"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(serviceUnavailable().withBody(MAPPER.writeValueAsString(createResponse("bad request")))));
        try {
            client.getPetById(1);
        } catch (Exception e) {
            //5 is number of retries, so 1+5
            verify(exactly(6), getRequestedFor(urlEqualTo("/pets/1")));
        }
    }

}