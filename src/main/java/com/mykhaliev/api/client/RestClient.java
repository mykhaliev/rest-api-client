package com.mykhaliev.api.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mykhaliev.api.client.model.api.RequestMethod;

import java.io.File;

/**
 * Abstract rest client interface.
 */
public interface RestClient {

    /**
     * Get request.
     *
     * @param path         path
     * @param responseType response type
     * @return response
     */
    default <T> T get(String path, TypeReference<T> responseType) {
        return callWithJson(RequestMethod.GET, path, responseType, null);
    }

    /**
     * Post.
     *
     * @param path         path
     * @param responseType response type
     * @param payload      payload
     * @return response
     */
    default <T> T post(String path, TypeReference<T> responseType, Object payload) {
        return callWithJson(RequestMethod.POST, path, responseType, payload);
    }

    /**
     * Post request.
     *
     * @param path         path
     * @param responseType response type
     * @param payload      file for payload
     * @return response
     */
    default <T> T postFile(String path, TypeReference<T> responseType, File payload) {
        return callWithMultipart(RequestMethod.POST, path, responseType, payload);
    }

    /**
     * Put request.
     *
     * @param path         path
     * @param responseType response type
     * @param payload      payload
     * @return the response
     */
    default <T> T put(String path, TypeReference<T> responseType, Object payload) {
        return callWithJson(RequestMethod.PUT, path, responseType, payload);
    }

    /**
     * Patch request.
     *
     * @param path    path
     * @param payload the payload
     * @return response
     */
    default <T> T patch(String path, TypeReference<T> responseType, Object payload) {
        return callWithJson(RequestMethod.PATCH, path, responseType, payload);
    }

    /**
     * Delete request.
     *
     * @param path path
     * @return response
     */
    default Object delete(String path) {
        return callWithJson(RequestMethod.DELETE, path, null, null);
    }

    /**
     * Generic json request.
     *
     * @param method       request method
     * @param path         path
     * @param responseType response type
     * @param payload      payload
     * @return response
     */
    <T> T callWithJson(RequestMethod method, String path, TypeReference<T> responseType, Object payload);

    /**
     * Generic multipart request.
     *
     * @param method       request method
     * @param path         path
     * @param responseType response type
     * @param payload      payload
     * @return response
     */
    <T> T callWithMultipart(RequestMethod method, String path, TypeReference<T> responseType, File payload);

    /**
     * Downloads file.
     *
     * @param url             file url
     * @param destinationPath file destination path
     */
    void downloadFile(String url, String destinationPath);

}
