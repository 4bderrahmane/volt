package com.volt.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defines which catalog HTTP endpoints are public and which Keycloak roles
 * are required for protected operations.
 *
 * <p>A {@link SecurityFilterChain} is the security gate in front of every
 * controller. It validates the bearer token before the controller runs, asks
 * {@link KeycloakRealmRoleConverter} to translate its roles, and then applies
 * the rules below. The API is stateless: it does not create a server session,
 * because every request carries its own signed access token.
 */
@Configuration
public class SecurityConfiguration {

    /**
     * Builds the incoming-request security filter used by catalog-service.
     *
     * <p>Reading products remains public. Product changes require an ADMIN
     * token, while internal machine-to-machine routes require SERVICE. Health
     * and OpenAPI routes stay public so Compose and developers can inspect the
     * service without first logging in.
     */
    @Bean
    SecurityFilterChain catalogSecurityFilterChain(
            HttpSecurity http,
            KeycloakRealmRoleConverter keycloakRoles) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakRoles);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/internal/**")
                        .hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/brands")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
