package com.volt.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * Health and OpenAPI routes stay public so Compose health checks and
     * developers can inspect the service without first logging in.
     */
    @Bean
    SecurityFilterChain catalogSecurityFilterChain(
            HttpSecurity http,
            KeycloakRealmRoleConverter keycloakRoles) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakRoles);

        try {
            http
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
                            .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/brands")
                            .permitAll()
                            .requestMatchers("/api/v1/**", "/internal/**")
                            .authenticated()
                            .anyRequest()
                            .denyAll())
                    .oauth2ResourceServer(resourceServer -> resourceServer
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Error configuring SecurityFilterChain", e);
        }
    }
}
