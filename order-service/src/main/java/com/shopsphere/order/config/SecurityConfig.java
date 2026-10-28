package com.shopsphere.order.config;

import com.shopsphere.common.web.JsonSecurityErrorHandler;
import com.shopsphere.common.web.ResourceServerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    /** Scope rules per HTTP method. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler,
                                           JwtAuthenticationConverter converter) throws Exception {
        ResourceServerSupport.apply(http, errorHandler, converter);
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAuthority("SCOPE_shop.read")
                .requestMatchers("/api/v1/orders/**").hasAuthority("SCOPE_shop.write")
                .anyRequest().authenticated());
        return http.build();
    }
}
