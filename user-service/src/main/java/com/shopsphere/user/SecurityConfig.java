package com.shopsphere.user;

import com.shopsphere.common.web.JsonSecurityErrorHandler;
import com.shopsphere.common.web.ResourceServerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** User Service access rules: reads need the shop.read scope, changes need shop.write. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Scope rules per HTTP method; machine tokens (scope "internal") get 403. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler,
                                           JwtAuthenticationConverter converter) throws Exception {
        ResourceServerSupport.apply(http, errorHandler, converter);
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAuthority("SCOPE_shop.read")
                .requestMatchers("/api/v1/users/**").hasAuthority("SCOPE_shop.write")
                .anyRequest().authenticated());
        return http.build();
    }

}
