package com.shopsphere.product.config;

import com.shopsphere.common.web.JsonSecurityErrorHandler;
import com.shopsphere.common.web.ResourceServerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Security rules for the Product Service.
 *
 * <p>The service is an OAuth2 "resource server": it trusts JWTs signed by the Auth
 * Server. It verifies each token locally (signature, issuer, expiry) using the public
 * keys it downloads from the Auth Server, with no per-request call. Rules are
 * "default deny": anything not explicitly permitted needs a valid token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /** Public: catalog reads, health, API docs. Everything else needs a valid token. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler,
                                           JwtAuthenticationConverter converter) throws Exception {
        ResourceServerSupport.apply(http, errorHandler, converter);
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
