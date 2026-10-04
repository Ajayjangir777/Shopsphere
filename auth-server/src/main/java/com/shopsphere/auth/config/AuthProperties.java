package com.shopsphere.auth.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Type-safe binding for the {@code auth.*} properties served by the Config Server.
 *
 * @param issuer            public URL of this server; it becomes the "iss" claim in every token
 * @param serviceClientSecret secret for the machine-to-machine client (client credentials flow)
 * @param webRedirectUris   URLs the React app may be redirected to after login
 * @param seedDemoUsers     when true, demo users are created on startup (development only)
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        String issuer,
        String serviceClientSecret,
        List<String> webRedirectUris,
        boolean seedDemoUsers
) {



}
