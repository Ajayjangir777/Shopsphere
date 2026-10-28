package com.shopsphere.order.feignClient;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that the caller's validated token is relayed, and that nothing is invented when there is none. */
class TokenRelayInterceptorTest {

    private final TokenRelayInterceptor interceptor = new TokenRelayInterceptor();

    /** The SecurityContext is thread-local: always clean up so tests cannot affect each other. */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** A JWT-authenticated request produces a Bearer header with the same token. */
    @Test
    void relaysTheCallersBearerToken() {
        Jwt jwt = Jwt.withTokenValue("abc.def.ghi").header("alg", "RS256").subject("alice").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc.def.ghi");
    }

    /** Without an authenticated user, no Authorization header is added. */
    @Test
    void withoutAuthentication_addsNoHeader() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}