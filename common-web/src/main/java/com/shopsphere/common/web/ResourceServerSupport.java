package com.shopsphere.common.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Shared "resource server" setup so each service's SecurityConfig only declares its own
 * URL rules: stateless, no CSRF (tokens are not cookies), JWT validation, JSON 401/403.
 */
public class ResourceServerSupport {

    private ResourceServerSupport() {}

    /**
     * Applies the common settings to the given security builder.
     *
     * @param http         the builder to configure
     * @param errorHandler writes the 401 and 403 bodies
     * @param converter    maps JWT claims to authorities
     * @throws Exception if the configuration is invalid
     */
    public static void apply(HttpSecurity http, JsonSecurityErrorHandler errorHandler,
                             JwtAuthenticationConverter converter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
    }
}
