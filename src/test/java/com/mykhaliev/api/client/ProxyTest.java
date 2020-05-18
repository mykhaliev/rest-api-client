package com.mykhaliev.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.mykhaliev.api.client.config.RestApiClientConfig;
import com.mykhaliev.api.client.model.api.Response;
import com.mykhaliev.api.client.model.dto.Pet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.mykhaliev.api.client.AbstractRestServiceTest.createResponse;


public class ProxyTest {

    WireMockServer proxy = new WireMockServer(options().port(9999).enableBrowserProxying(true));
    WireMockServer acServerMocked = new WireMockServer(options().port(8089));

    @Test
    public void proxyTest() throws JsonProcessingException {
        proxy.start();
        acServerMocked.start();
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("pet");

        RestApiClientConfig testConfig = RestApiClientConfig
                .builder()
                .apiRootUrl("http://localhost:8089")
                .username("username")
                .password("password")
                .retryCount(5)
                .retryIntervalMilliseconds(1)
                .proxyHost("http://localhost")
                .proxyPort(9999)
                .build();

        PetstoreRestClient client = new PetstoreRestClient(testConfig);

        List<Pet> pets = Collections.singletonList(pet);

        acServerMocked.stubFor(get(urlEqualTo("/pets"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(aResponse().withBody(AbstractRestServiceTest.MAPPER.writeValueAsString(createResponse(pets)))));

        Response<List<Pet>> response = client.getPets(1);
        Assert.assertEquals(1, response.getResult().size());
        Assert.assertEquals(Long.valueOf(1L), response.getResult().get(0).getId());
        Assert.assertEquals("pet", response.getResult().get(0).getName());

        proxy.stop();
        acServerMocked.stop();
    }
}
