package com.volt.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Places Keycloak authentication and role checks in front of order-service's
 * future HTTP input adapters.
 *
 * <p>CLIENT users can use normal order routes. Changing an order's status is
 * an administrative operation and therefore requires ADMIN. The filter chain
 * validates the JWT before a controller or use case sees the request, keeping
 * authentication concerns out of the hexagon's application and domain layers.
 */
@Configuration
public class SecurityConfiguration {

    /** Builds the stateless bearer-token filter for incoming order requests. */
    @Bean
    SecurityFilterChain orderSecurityFilterChain(
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
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/**")
                        .hasRole("CLIENT")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
