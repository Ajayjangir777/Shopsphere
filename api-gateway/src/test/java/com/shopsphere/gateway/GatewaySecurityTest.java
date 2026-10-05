package com.shopsphere.gateway;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Verifies the gateway's edge security with a running (random-port) application.
 * Token decoding is mocked, so neither the Auth Server nor Redis is needed.
 * No routes exist in this test, so a request that passes security answers 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewaySecurityTest {

    @Autowired
    WebTestClient client;

    /** Stands in for the real decoder that would download keys from the Auth Server. */
    @MockBean
    ReactiveJwtDecoder jwtDecoder;

    /** Teaches the fake decoder one good token and one bad one. */
    @BeforeEach
    void stubDecoder() {
        Jwt jwt = Jwt.withTokenValue("good-token")
                .header("alg", "RS256")
                .subject("customer")
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("good-token")).thenReturn(Mono.just(jwt));
        when(jwtDecoder.decode("garbage")).thenReturn(Mono.error(new BadJwtException("invalid")));
    }

    /** Browsing is public: security lets it through (404 only because no route exists here). */
    @Test
    void publicRead_passesSecurity() {
        client.get().uri("/api/v1/products").exchange().expectStatus().isNotFound();
    }

    /** Sign-up is public too. */
    @Test
    void register_passesSecurity() {
        client.post().uri("/api/auth/register").exchange().expectStatus().isNotFound();
    }

    /** A write without a token is stopped at the edge with a JSON 401. */
    @Test
    void write_withoutToken_gets401() {
        client.post().uri("/api/v1/products").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectBody().jsonPath("$.title").isEqualTo("Authentication required");
    }

    /** A token that fails verification is treated like no token: 401. */
    @Test
    void write_withInvalidToken_gets401() {
        client.post().uri("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
                .exchange().expectStatus().isUnauthorized();
    }

    /** A valid token passes the gateway; role checks are the service's job. */
    @Test
    void write_withValidToken_passesSecurity() {
        client.post().uri("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .exchange().expectStatus().isNotFound();
    }

    /** A browser preflight carries no token and must still succeed with CORS headers. */
    @Test
    void corsPreflight_succeedsWithoutToken() {
        client.options().uri("/api/v1/products")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
    }
}
