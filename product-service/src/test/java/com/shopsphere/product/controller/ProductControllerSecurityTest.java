package com.shopsphere.product.controller;

import com.shopsphere.product.dto.PageResponse;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.exception.GlobalExceptionHandler;
import com.shopsphere.product.security.JsonSecurityErrorHandler;
import com.shopsphere.product.config.SecurityConfig;
import com.shopsphere.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the access rules of the product API without a database, Redis or Auth
 * Server: only the web layer and security are loaded, the service is mocked, and
 * tokens are simulated with Spring Security's test support.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class, GlobalExceptionHandler.class})
public class ProductControllerSecurityTest {

    private static final String VALID_BODY = """
            {"name":"iPhone 15","description":"128GB","price":79999.00,"category":"mobiles","stock":25}""";

    @Autowired
    MockMvc mockMvc;

    /** Replaces the real service so no database is needed. */
    @MockBean
    ProductService productService;

    /** Satisfies the resource-server setup; the tests skip real token decoding. */
    @MockBean
    JwtDecoder jwtDecoder;

    /** Authorities of an admin whose token includes the write scope. */
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority CUSTOMER = new SimpleGrantedAuthority("ROLE_CUSTOMER");
    private static final SimpleGrantedAuthority WRITE_SCOPE = new SimpleGrantedAuthority("SCOPE_shop.write");

    /** Anonymous visitors can browse the catalog. */
    @Test
    void anonymous_canListProducts() throws Exception {
        when(productService.list(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }

    /** No token on a write gives 401, a Bearer challenge, and a JSON body. */
    @Test
    void anonymous_cannotCreate_gets401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    /** A logged-in customer is known but not allowed: 403, not 401. */
    @Test
    void customer_cannotCreate_gets403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(CUSTOMER, WRITE_SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    /** An admin whose token lacks the write scope is refused: both conditions are needed. */
    @Test
    void adminWithoutWriteScope_cannotCreate_gets403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    /** Admin role plus write scope may create. */
    @Test
    void adminWithWriteScope_canCreate() throws Exception {
        when(productService.create(any())).thenReturn(new ProductResponse(
                UUID.randomUUID(), "iPhone 15", "128GB", new BigDecimal("79999.00"),
                "mobiles", 25, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(ADMIN, WRITE_SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    /** Updates follow the same rule as creates. */
    @Test
    void customer_cannotUpdate_gets403() throws Exception {
        mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                        .with(jwt().authorities(CUSTOMER, WRITE_SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    /** Deletes: anonymous gets 401, admin with scope gets 204. */
    @Test
    void delete_requiresAdminWithWriteScope() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/" + id)).andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/products/" + id)
                        .with(jwt().authorities(ADMIN, WRITE_SCOPE)))
                .andExpect(status().isNoContent());
    }
}
