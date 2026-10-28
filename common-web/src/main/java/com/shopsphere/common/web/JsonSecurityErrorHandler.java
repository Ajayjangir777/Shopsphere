package com.shopsphere.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

import static java.nio.file.Files.write;

/**
 * Writes JSON (RFC 7807) bodies for security failures: 401 when the caller is not
 * authenticated, 403 when authenticated but not allowed. Used by every service.
 */

public class JsonSecurityErrorHandler implements AuthenticationEntryPoint , AccessDeniedHandler {


    private final ObjectMapper objectMapper;

    /** Creates the handler with the application's JSON mapper. */
    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Unauthenticated request (no, malformed, forged or expired token): 401 with a Bearer challenge. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "Authentication required", "A valid access token is required");
    }

    /** Authenticated but not permitted: 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Access denied", "You do not have permission to perform this action");
    }

    /** Writes a problem document with the given status as the response body. */
    private void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

}
