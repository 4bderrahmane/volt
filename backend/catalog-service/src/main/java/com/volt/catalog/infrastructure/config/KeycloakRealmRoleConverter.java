package com.volt.catalog.infrastructure.config;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter between Keycloak's token format and Spring Security's role format.
 *
 * <p>Keycloak stores realm roles inside a nested claim such as
 * {@code realm_access.roles = [ADMIN, CLIENT]}. Spring Security, however,
 * makes authorization decisions using authorities such as {@code ROLE_ADMIN}.
 * This converter translates between those two vocabularies while preserving
 * normal OAuth scopes such as {@code SCOPE_openid}.
 */
@Component
public final class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);
        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }

        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null || !(realmAccess.get(ROLES_CLAIM) instanceof Collection<?> roles)) {
            return authorities;
        }

        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }
}
