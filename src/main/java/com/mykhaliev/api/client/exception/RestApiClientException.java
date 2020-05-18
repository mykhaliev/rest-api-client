package com.mykhaliev.api.client.exception;


import com.mykhaliev.api.client.model.api.Response;

/**
 * Rest client exception.
 */
public class RestApiClientException extends RuntimeException {

    public RestApiClientException(String url, int statusCode, Response errorResponse) {
        super(String.format("[url: %s, error code: %d, response: %s]", url, statusCode, errorResponse));
    }

    public RestApiClientException(String message) {
        super(message);
    }

    public RestApiClientException(Throwable cause) {
        super(cause);
    }

}
