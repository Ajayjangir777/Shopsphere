package com.shopsphere.order.feignClient;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Copies the caller's validated JWT onto an outgoing Feign request so the downstream
 * service sees the same user. It reads the SecurityContext (thread-local), so it only works
 * while the Feign call runs on the request thread.
 */
public class TokenRelayInterceptor implements RequestInterceptor {

    /** Adds "Authorization: Bearer ..." when the current request is authenticated with a JWT. */
    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuth.getToken().getTokenValue());
        }
    }
}