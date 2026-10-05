package com.shopsphere.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.nio.file.Files.write;

/**
 * Reactive counterpart of the Product Service's error handler: writes RFC 7807 JSON
 * bodies for security failures at the gateway (401 when the caller cannot be
 * authenticated, 403 when authenticated but not allowed).
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {


    /** Serializes the error document to JSON. */
    private final ObjectMapper objectMapper;

    /**
     * Handles an unauthenticated request (missing, malformed, forged or expired token).
     *
     * @param exchange the current request/response pair
     * @param ex       why authentication failed
     * @return a Mono that completes once the 401 response is written
     */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return write(exchange, HttpStatus.UNAUTHORIZED, "Authentication required",
                "A valid access token is required");
    }

    /**
     * Handles an authenticated caller who lacks permission.
     *
     * @param exchange the current request/response pair
     * @param denied   the access-denied reason (not exposed to the client)
     * @return a Mono that completes once the 403 response is written
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return write(exchange, HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action");
    }

    /**
     * Writes a problem document as the response body, without blocking.
     *
     * @param exchange the current request/response pair
     * @param status   the HTTP status to send
     * @param title    short summary of the problem
     * @param detail   human-readable explanation
     * @return a Mono that completes when the body has been written
     */
    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        try {
            byte[] body = objectMapper.writeValueAsBytes(problem);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (JsonProcessingException ex) {
            return Mono.error(ex);
        }
    }

}
