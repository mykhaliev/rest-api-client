package com.mykhaliev.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mykhaliev.api.client.model.api.Response;
import com.mykhaliev.api.client.model.dto.Pet;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class PetstoreRestClientTest extends AbstractRestServiceTest {

    @Test
    public void getPetsTest() throws JsonProcessingException {
        Pet pet = new Pet(1L, "pet");

        stubFor(get(urlEqualTo("/pets"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(aResponse().withBody(MAPPER.writeValueAsString(createResponse(Collections.singletonList(pet))))));

        Response<List<Pet>> response = client.getPets(1);
        Assert.assertEquals(1, response.getResult().size());
        Assert.assertEquals(Long.valueOf(1L), response.getResult().get(0).getId());
        Assert.assertEquals("pet", response.getResult().get(0).getName());
    }

    @Test
    public void getSinglePetTest() throws JsonProcessingException {
        Pet pet = new Pet(1L, "pet");

        stubFor(get(urlEqualTo("/pets/1"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(aResponse().withBody(MAPPER.writeValueAsString(createResponse(pet)))));

        Response<Pet> response = client.getPetById(1);
        Assert.assertEquals(Long.valueOf(1L), response.getResult().getId());
        Assert.assertEquals("pet", response.getResult().getName());
    }

    @Test
    public void savePetTest() throws JsonProcessingException {
        Pet pet = new Pet(1L, "pet");

        stubFor(post(urlEqualTo("/pets"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .withRequestBody(matchingJsonPath("$[?(@.name=='pet')]"))
                .willReturn(aResponse().withBody(MAPPER.writeValueAsString(createResponse(pet)))));

        Response<Pet> response = client.savePet(pet);
        Assert.assertEquals(Long.valueOf(1L), response.getResult().getId());
        Assert.assertEquals("pet", response.getResult().getName());
    }

    @Test
    public void updatePetTest() throws JsonProcessingException {
        Pet pet = new Pet(1L, "pet");

        stubFor(put(urlEqualTo("/pets/1"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .withRequestBody(matchingJsonPath("$[?(@.name=='pet')]"))
                .willReturn(aResponse().withBody(MAPPER.writeValueAsString(createResponse(pet)))));

        Response<Pet> response = client.updatePet(1, pet);
        Assert.assertEquals(Long.valueOf(1L), response.getResult().getId());
        Assert.assertEquals("pet", response.getResult().getName());
    }

    @Test
    public void removePetTest() {
        stubFor(delete(urlEqualTo("/pets/1"))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .willReturn(aResponse().withStatus(204)));

        client.removePet(1);
    }

    @Test
    public void uploadPetInfoTest() throws JsonProcessingException {
        stubFor(post(urlEqualTo("/pets/1/info"))
                .withHeader("Accept", equalTo("application/json"))
                .withBasicAuth("username", "password")
                .withMultipartRequestBody(aMultipart().withName("file"))
                .willReturn(aResponse().withBody(MAPPER.writeValueAsString(createResponse(new Pet(1L, "pet"))))));
        File infoFile = new File(this.getClass().getClassLoader().getResource("dummy.txt").getFile());

        Response<Pet> response = client.uploadPetInfo(1, infoFile);
        Assert.assertEquals(Long.valueOf(1L), response.getResult().getId());
        Assert.assertEquals("pet", response.getResult().getName());
    }

    @Test
    public void downloadPetInfoTest() throws IOException {
        stubFor(get(urlEqualTo("/pets/1/info"))
                .withBasicAuth("username", "password")
                .willReturn(aResponse()
                        .withBody("test")));
        File tempFile = File.createTempFile("temp-", "-temp");

        client.downloadPetInfo(1, tempFile.getPath().toString());
        Assert.assertEquals("test", FileUtils.readFileToString(tempFile, StandardCharsets.UTF_8));
    }
}