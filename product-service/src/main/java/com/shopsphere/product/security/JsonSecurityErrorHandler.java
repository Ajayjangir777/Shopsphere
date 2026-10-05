package com.shopsphere.product.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static java.nio.file.Files.write;

/**
 * Produces consistent JSON error bodies (RFC 7807) for security failures, instead of
 * Spring Security's default empty responses. This lets the React app handle every
 * error the same way.
 *
 * <p>It plays two roles: the "authentication entry point" (called when the caller is
 * not authenticated, giving 401) and the "access denied handler" (called when the
 * caller is authenticated but not allowed, giving 403).
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** Serializes the error document to JSON. */
    private final ObjectMapper objectMapper;

    /**
     * Handles an unauthenticated request: no token, a malformed token, a forged
     * signature, or an expired token. Responds 401 and tells the client a Bearer
     * token is expected.
     *
     * @param request   the rejected request
     * @param response  the response to write
     * @param authException why authentication failed
     * @throws IOException if writing the body fails
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "Authentication required",
                "A valid access token is required");
    }

    /**
     * Handles an authenticated caller who lacks permission. Responds 403.
     *
     * @param request  the rejected request
     * @param response the response to write
     * @param exception the access-denied reason (not exposed to the client)
     * @throws IOException if writing the body fails
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action");
    }

    /**
     * Writes a problem document with the given status as the response body.
     *
     * @param response the response to write to
     * @param status   the HTTP status to send
     * @param title    short summary of the problem
     * @param detail   human-readable explanation
     * @throws IOException if writing fails
     */
    private void write(HttpServletResponse response, HttpStatus status,
                       String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
