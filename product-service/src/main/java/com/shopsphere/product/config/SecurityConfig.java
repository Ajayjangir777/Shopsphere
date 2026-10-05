package com.shopsphere.product.config;

import com.shopsphere.product.security.JsonSecurityErrorHandler;
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

    /**
     * Defines which requests are public and which need a token.
     *
     * <p>This URL-level layer is the coarse rule (default deny); the fine-grained role
     * checks live on the controller methods as {@code @RequiresShopAdmin}.
     *
     * @param http         the security builder
     * @param errorHandler writes the JSON bodies for 401 and 403
     * @param converter    turns a verified JWT into Spring authorities
     * @return the configured filter chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JsonSecurityErrorHandler errorHandler,
                                           JwtAuthenticationConverter converter) throws Exception {
        http
                // Tokens travel in an Authorization header that browsers do NOT attach
                // automatically, so there is no CSRF risk and no session is needed.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // DEV convenience: API docs open. Restrict or remove in production.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Browsing the catalog needs no login.
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .anyRequest().authenticated())
                // 401 / 403 for requests that reach the authorization step without a token or permission.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // Validate Bearer JWTs; the same JSON handler covers bad or expired tokens.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    /**
     * Maps JWT claims to Spring Security authorities:
     * <ul>
     *   <li>each scope "shop.write" becomes {@code SCOPE_shop.write}</li>
     *   <li>each entry of our custom "roles" claim, e.g. "ADMIN", becomes {@code ROLE_ADMIN}</li>
     * </ul>
     * Those prefixes are what {@code hasAuthority(...)} and {@code hasRole(...)} look for.
     *
     * @return the converter used after a token has been verified
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));

            List<String> roles = jwt.getClaimAsStringList("roles");   // null for machine tokens
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            return authorities;
        });
        return converter;
    }
}
