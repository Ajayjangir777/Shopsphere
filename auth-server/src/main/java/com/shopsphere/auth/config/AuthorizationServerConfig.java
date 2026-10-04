package com.shopsphere.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OAuth2-specific configuration: which clients may use this server, how tokens are
 * signed, and what extra information is placed inside access tokens.
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     * Registers the two OAuth2 clients known to this server.
     *
     * <ul>
     *   <li><b>shopsphere-web</b>: the React app. A PUBLIC client (it cannot keep a secret
     *       because its code runs in the browser), so it uses Authorization Code + PKCE.</li>
     *   <li><b>shopsphere-service</b>: a CONFIDENTIAL client for service-to-service calls
     *       with no user involved (Client Credentials).</li>
     * </ul>
     *
     * <p>Clients live in memory for now; they are re-created on every restart.
     *
     * @param props   our settings (redirect URIs and the service client's secret)
     * @param encoder hashes the service client's secret
     * @return the repository the server consults for every client request
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(AuthProperties props,
                                                                 PasswordEncoder encoder) {
        RegisteredClient webClient = RegisteredClient.withId("shopsphere-web-id")
                .clientId("shopsphere-web")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)   // public client: no secret
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(uris -> uris.addAll(props.webRedirectUris()))
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("shop.read")
                .scope("shop.write")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)                // PKCE is mandatory
                        .requireAuthorizationConsent(false)   // first-party app: no consent screen
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))   // short-lived by design
                        .refreshTokenTimeToLive(Duration.ofHours(8))
                        .reuseRefreshTokens(false)            // rotate: each refresh issues a NEW refresh token
                        .build())
                .build();

        RegisteredClient serviceClient = RegisteredClient.withId("shopsphere-service-id")
                .clientId("shopsphere-service")
                .clientSecret(encoder.encode(props.serviceClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("internal")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient, serviceClient);
    }

    /**
     * Tells the server its own public identity. The issuer URL is written into every
     * token and published in the discovery document; resource servers must be
     * configured with exactly the same value.
     *
     * @param props our settings holding the issuer URL
     * @return the server settings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthProperties props) {
        return AuthorizationServerSettings.builder().issuer(props.issuer()).build();
    }


    /**
     * Creates the key pair used to sign tokens and exposes the PUBLIC half at
     * {@code /oauth2/jwks}. Other services download that public key to verify signatures.
     *
     * <p>DEV LIMITATION: a new key is generated on every start, so tokens issued before a
     * restart stop being valid. Production loads persistent keys (AWS Secrets Manager/KMS)
     * and rotates them on a schedule.
     *
     * @return the source of signing keys
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())   // "kid": lets verifiers pick the right key after rotation
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * Generates a 2048-bit RSA key pair.
     *
     * @return a fresh key pair
     */
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }

    /**
     * Decoder that verifies JWTs signed with our own keys. Needed by the /userinfo endpoint
     * (which accepts our access tokens) and by the integration test.
     *
     * @param jwkSource the signing keys
     * @return a decoder trusting this server's keys
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Adds a {@code roles} claim (e.g. ["ADMIN"]) to user access tokens, so resource servers
     * can make authorization decisions without calling this server. Machine-to-machine
     * tokens have no user, so they get no roles.
     *
     * @return the customizer invoked each time a JWT is built
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            boolean isAccessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
            boolean isUserToken = !AuthorizationGrantType.CLIENT_CREDENTIALS
                    .equals(context.getAuthorizationGrantType());

            if (isAccessToken && isUserToken) {
                Set<String> roles = context.getPrincipal().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length()))
                        .collect(Collectors.toSet());
                context.getClaims().claim("roles", roles);
            }
        };
    }

}
