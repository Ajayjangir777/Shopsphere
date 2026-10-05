package com.shopsphere.gateway.config;


import com.shopsphere.gateway.security.JsonSecurityErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Security for the gateway: the first line of defence at the edge.
 *
 * <p>The gateway only AUTHENTICATES (is this a valid, unexpired token from our Auth
 * Server?). Role checks stay in each service, because the service owns its own rules
 * and must stay safe even if someone reaches it without going through the gateway.
 * The Authorization header is forwarded to the services unchanged.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * Defines public paths and requires a valid token for everything else.
     * Runs BEFORE the gateway's routing, so rejected requests never reach a service.
     *
     * @param http         the reactive security builder
     * @param errorHandler writes the JSON bodies for 401 and 403
     * @param corsSource   CORS rules, applied before authentication so preflights succeed
     * @return the configured filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JsonSecurityErrorHandler errorHandler,
                                                         CorsConfigurationSource corsSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)   // bearer tokens, no cookies
                .cors(cors -> cors.configurationSource(corsSource))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    /**
     * Declares which web origins (React dev servers) may call the API from a browser,
     * with which methods and headers.
     *
     * <p>Credentials are allowed, so origins must be listed explicitly: a wildcard is
     * not permitted together with credentials. The exposed headers let the React app
     * read the correlation ID and rate-limit information from responses.
     *
     * @param allowedOrigins comma-separated origins from {@code gateway.cors.allowed-origins}
     * @return the CORS rules applied to every path
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${gateway.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "X-RateLimit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));   // browsers may cache the preflight answer for an hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
