package com.shopsphere.auth;


import com.shopsphere.auth.user.AppUser;
import com.shopsphere.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the sign-up endpoint against a real PostgreSQL
 * (Testcontainers): success, privilege-escalation attempts, duplicates, validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class RegistrationIntegrationTest {

    /** Throwaway PostgreSQL; @ServiceConnection wires its URL and credentials automatically. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    /** Happy path: 201, no password in the response, CUSTOMER role, password stored hashed. */
    @Test
    void register_createsCustomerWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("alice_01", "alice@example.com", "Str0ngPassw0rd")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("alice_01"))
                .andExpect(jsonPath("$.password").doesNotExist());

        AppUser saved = userRepository.findByUsername("alice_01").orElseThrow();
        assertThat(saved.getRoles()).containsExactly("CUSTOMER");
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("Str0ngPassw0rd");
        assertThat(passwordEncoder.matches("Str0ngPassw0rd", saved.getPasswordHash())).isTrue();
    }

    /** A client trying to grant itself ADMIN must be ignored: the field is not part of the API. */
    @Test
    void register_ignoresRolesSentByClient() throws Exception {
        String evilBody = """
                {"username":"mallory_01","email":"mallory@example.com",
                 "password":"Str0ngPassw0rd","roles":["ADMIN"]}""";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evilBody))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByUsername("mallory_01").orElseThrow().getRoles())
                .containsExactly("CUSTOMER");
    }

    /** "Bob_01" and "bob_01" must be the same account, otherwise look-alike names are possible. */
    @Test
    void register_rejectsDuplicateUsernameIgnoringCase() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Bob_01", "bob@example.com", "Str0ngPassw0rd")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("bob_01", "other@example.com", "Str0ngPassw0rd")))
                .andExpect(status().isConflict());
    }

    /** The same email cannot be used for two accounts. */
    @Test
    void register_rejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("carol_01", "carol@example.com", "Str0ngPassw0rd")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("carol_02", "CAROL@example.com", "Str0ngPassw0rd")))
                .andExpect(status().isConflict());
    }

    /** Bad input must give 400 with one message per field, and must not echo the password. */
    @Test
    void register_rejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("a!", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(content().string(not(containsString("short"))));
    }

    /** Builds the JSON request body used by the tests. */
    private static String body(String username, String email, String password) {
        return String.format("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                username, email, password);
    }

}
