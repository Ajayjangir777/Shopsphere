package com.shopsphere.order.integration;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Test helper that builds the exceptions Feign throws, without any real HTTP traffic. */
public class FeignErrors {

    private FeignErrors() {}

    /** The FeignException Feign throws for a response with the given status (404 gives NotFound, etc.). */
    static FeignException status(int status) {
        Response response = Response.builder().status(status).reason("test")
                .request(request()).headers(Map.of()).build();
        return FeignException.errorStatus("Client#method", response);
    }

    /** What Feign throws when it cannot reach the server at all: a RetryableException with status -1. */
    static RetryableException connectionRefused() {
        return new RetryableException(-1, "Connection refused", Request.HttpMethod.GET,
                new ConnectException("refused"), (Long) null, request());
    }

    /** A dummy request object that Feign's exceptions require. */
    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://service/x", Map.of(), null,
                StandardCharsets.UTF_8, null);
    }



}
