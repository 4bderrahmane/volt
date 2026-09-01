package com.volt.order.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class CatalogOAuthClientConfiguration {

    private static final String CATALOG_REGISTRATION_ID = "catalog";
    private static final Authentication ORDER_SERVICE_PRINCIPAL = new AnonymousAuthenticationToken(
            "order-service-client",
            "order-service",
            AuthorityUtils.createAuthorityList("ROLE_SERVICE"));

    @Bean
    OAuth2AuthorizedClientManager catalogAuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /** Uses one fixed service principal so tokens are not cached per browser user. */
    @Bean
    @Qualifier("catalogRestClient")
    RestClient catalogRestClient(
            OAuth2AuthorizedClientManager catalogAuthorizedClientManager,
            OAuth2AuthorizedClientService authorizedClients,
            @Value("${volt.catalog.base-url}") String catalogBaseUrl,
            @Value("${volt.catalog.connect-timeout:PT1S}") Duration connectTimeout,
            @Value("${volt.catalog.read-timeout:PT3S}") Duration readTimeout) {
        OAuth2ClientHttpRequestInterceptor oauth =
                new OAuth2ClientHttpRequestInterceptor(catalogAuthorizedClientManager);
        oauth.setClientRegistrationIdResolver(request -> CATALOG_REGISTRATION_ID);
        oauth.setPrincipalResolver(request -> ORDER_SERVICE_PRINCIPAL);
        oauth.setAuthorizationFailureHandler(
                OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClients));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(catalogBaseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(oauth)
                .build();
    }
}
