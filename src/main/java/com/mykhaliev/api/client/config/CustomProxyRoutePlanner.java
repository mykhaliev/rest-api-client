package com.mykhaliev.api.client.config;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;


/**
 * Proxy planner for http client.
 */
public class CustomProxyRoutePlanner extends DefaultProxyRoutePlanner {


    public CustomProxyRoutePlanner(HttpHost proxy) {
        super(proxy);
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
        if (host == null || host.getHostName() == null) {
            return super.determineRoute(host, request, context);
        }
        return new HttpRoute(host);
    }
}
