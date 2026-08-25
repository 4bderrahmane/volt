package com.volt.catalog.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the small but critical translation between a real Keycloak-shaped
 * token and the authorities used by catalog-service's access rules.
 */
class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void convertsRealmRolesAndKeepsOauthScopes() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-08-14T10:00:00Z"))
                .expiresAt(Instant.parse("2026-08-14T10:05:00Z"))
                .claim("scope", "openid profile")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "client")))
                .build();

        Collection<GrantedAuthority> converted = converter.convert(jwt);
        Set<String> authorities = converted.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("SCOPE_openid", "SCOPE_profile", "ROLE_ADMIN", "ROLE_CLIENT"), authorities);
    }

    @Test
    void acceptsATokenWithoutRealmRoles() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("scope", "openid")
                .build();

        Set<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("SCOPE_openid"), authorities);
    }
}
