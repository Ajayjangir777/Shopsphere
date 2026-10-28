package com.shopsphere.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class JwtConverterTest {

    private final JwtAuthenticationConverter converter = new CommonWebAutoConfiguration().jwtAuthenticationConverter();

    /** A user token has scopes and roles: both become authorities. */
    @Test
    void mapsScopesAndRolesToAuthorities() {
        Jwt jwt = jwtWith(Map.of("scope", List.of("shop.read", "shop.write"), "roles", List.of("ADMIN")));
        assertThat(converter.convert(jwt).getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_shop.read", "SCOPE_shop.write", "ROLE_ADMIN");
    }

    /** A machine token has no roles claim: only scopes, and no failure. */
    @Test
    void tokenWithoutRolesClaim_hasOnlyScopes() {
        Jwt jwt = jwtWith(Map.of("scope", List.of("internal")));
        assertThat(converter.convert(jwt).getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_internal");
    }

    /** Builds a minimal JWT carrying the given claims. */
    private static Jwt jwtWith(Map<String, Object> claims) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("user")
                .claims(c -> c.putAll(claims)).build();
    }
}
