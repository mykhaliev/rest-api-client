package com.mykhaliev.api.client.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Api response model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private int apiVersion;

    private T result;

    private Integer limit;

    private Long skip;

    private Long total;
}
