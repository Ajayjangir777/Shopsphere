package com.shopsphere.common.web;

import org.springframework.http.HttpStatus;

/**
 * An error a service raises on purpose, carrying the HTTP status it should produce.
 * {@link CommonExceptionAdvice} converts it to an RFC 7807 response, so services need
 * no exception-to-status mapping of their own.
 */
public class ApiException extends  RuntimeException {

    private final HttpStatus status;

    /** Creates an error with the status to return and a message that is safe to show clients. */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Returns the HTTP status this error maps to. */
    public HttpStatus getStatus() {
        return status;
    }

    /** 400: the request is malformed or inconsistent. */
    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }

    /** 404: the requested resource does not exist (or is not visible to the caller). */
    public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, message); }

    /** 409: a valid request that conflicts with the current state. */
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }

    /** 422: a well-formed request that refers to something invalid (unknown product, address). */
    public static ApiException unprocessable(String message) { return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message); }

    /** 503: a dependency we need is down or too slow. */
    public static ApiException unavailable(String message) { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
