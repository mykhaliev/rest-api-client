package com.mykhaliev.api.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mykhaliev.api.client.config.RestApiClientConfig;
import com.mykhaliev.api.client.model.api.Response;
import com.mykhaliev.api.client.model.dto.Pet;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

/**
 * Example of rest client usage.
 */
public class PetstoreRestClient {

    public static final String PETS_ROOT_PATH = "/pets";
    public static final String PET_ROOT_PATH = "/pets/{0,number,#}";
    public static final String PET_INFO_PATH = "/pets/{0,number,#}/info";

    private final RestClient client;

    public PetstoreRestClient(RestApiClientConfig config) {
        client = new RestApiClient(config);
    }


    public Response<Pet> getPetById(long id) {
        return client.get(MessageFormat.format(PET_ROOT_PATH, id), new TypeReference<Response<Pet>>() {
        });
    }

    public Response<List<Pet>> getPets(long id) {
        return client.get(PETS_ROOT_PATH, new TypeReference<Response<List<Pet>>>() {
        });
    }

    public Response<Pet> savePet(Pet pet) {
        return client.post(PETS_ROOT_PATH, new TypeReference<Response<Pet>>() {
        }, pet);
    }

    public Response<Pet> updatePet(long id, Pet pet) {
        return client.put(MessageFormat.format(PET_ROOT_PATH, id), new TypeReference<Response<Pet>>() {
        }, pet);
    }

    public void removePet(long id) {
        client.delete(MessageFormat.format(PET_ROOT_PATH, id));
    }

    public Response<Pet> uploadPetInfo(long petId, File infoFile) {
        return client.postFile(MessageFormat.format(PET_INFO_PATH, petId), new TypeReference<Response<Pet>>() {
        }, infoFile);
    }

    public void downloadPetInfo(long petId, String destinationPath) {
        client.downloadFile(MessageFormat.format(PET_INFO_PATH, petId), destinationPath);
    }

}