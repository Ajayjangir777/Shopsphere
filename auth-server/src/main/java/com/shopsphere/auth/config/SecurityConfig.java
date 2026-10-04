package com.shopsphere.auth.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Spring Security setup for the Authorization Server.
 *
 * <p>Two filter chains are needed because they protect different things:
 * chain 1 handles the OAuth2/OIDC protocol endpoints (/oauth2/**, /.well-known/**),
 * chain 2 handles everything else, including the human login page.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Chain 1: the OAuth2 and OpenID Connect endpoints (authorize, token, jwks, userinfo...).
     * Browser requests that are not logged in are redirected to the login page.
     *
     * @param http the security builder
     * @return the configured filter chain, checked first (order 1)
     * @throws Exception if the configuration is invalid
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Enable OpenID Connect 1.0 (adds ID tokens, /userinfo, discovery document).
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                // Not logged in + the client wants HTML => send them to the login form.
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                // /userinfo accepts a JWT access token issued by this very server.
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Chain 2: everything outside the protocol endpoints. Requires login (form login)
     * except for health checks and the public sign-up endpoint.
     *
     * <p>CSRF protection stays on for the login form, but is switched off for sign-up.
     * CSRF attacks work by making a victim's browser send a request together with its
     * automatic credentials (a session cookie). Sign-up uses no cookie or session at all,
     * so there is nothing for an attacker to ride on.
     *
     * @param http the security builder
     * @return the configured filter chain, checked second (order 2)
     * @throws Exception if the configuration is invalid
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/register"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }


    /**
     * Password hasher shared by user passwords and client secrets. The delegating encoder
     * writes hashes as "{bcrypt}..." so the algorithm can be upgraded later while old
     * hashes keep working.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
