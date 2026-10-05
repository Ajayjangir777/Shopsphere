package com.shopsphere.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the Authorization Server against a real PostgreSQL
 * (Testcontainers): protocol endpoints, token issuing, and seeded users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class AuthServerIntegrationTest {


    /** Throwaway PostgreSQL; @ServiceConnection wires its URL and credentials automatically. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    UserDetailsService userDetailsService;

    /** The OIDC discovery document must announce our issuer and where the public keys are. */
    @Test
    void discoveryDocument_announcesIssuerAndJwks() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:9000"))
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.token_endpoint").exists());
    }

    /** The JWKS endpoint must publish a public key and must never leak the private part. */
    @Test
    void jwks_publishesPublicKeyOnly() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    /** Client Credentials: a valid secret yields a verifiable JWT with the expected claims. */
    @Test
    void clientCredentials_issuesSignedJwt() throws Exception {
        String response = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basic("shopsphere-service", "test-secret"))
                        .param("grant_type", "client_credentials")
                        .param("scope", "internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn().getResponse().getContentAsString();

        String accessToken = JsonPath.read(response, "$.access_token");
        Jwt jwt = jwtDecoder.decode(accessToken);   // also verifies the signature

        assertThat(jwt.getIssuer().toString()).isEqualTo("http://localhost:9000");
        assertThat(jwt.getSubject()).isEqualTo("shopsphere-service");
        assertThat(jwt.getClaimAsStringList("scope")).containsExactly("internal");
    }

    /** A wrong client secret must be rejected with 401. */
    @Test
    void clientCredentials_rejectsWrongSecret() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basic("shopsphere-service", "wrong"))
                        .param("grant_type", "client_credentials")
                        .param("scope", "internal"))
                .andExpect(status().isUnauthorized());
    }

    /** A scope that was not registered for the client must be refused. */
    @Test
    void clientCredentials_rejectsUnregisteredScope() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basic("shopsphere-service", "test-secret"))
                        .param("grant_type", "client_credentials")
                        .param("scope", "shop.write"))
                .andExpect(status().isBadRequest());
    }

    /** Seeded admin must have the ADMIN role and a hashed (never plain) password. */
    @Test
    void seededAdmin_hasRoleAndHashedPassword() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(admin.getPassword()).startsWith("{bcrypt}").doesNotContain("admin123");
    }

    /** Unknown users must raise the exception Spring Security expects. */
    @Test
    void unknownUser_throwsUsernameNotFound() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    /** Builds the HTTP Basic header value used to authenticate a client. */
    private static String basic(String clientId, String secret) {
        String credentials = clientId + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
