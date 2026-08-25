package com.volt.order.infrastructure.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves that the outgoing catalog HTTP client performs the complete OAuth2
 * client-credentials flow.
 *
 * <p>The small local server plays both Keycloak and catalog-service. This keeps
 * the test fast and deterministic while checking real HTTP behavior: a token is
 * requested, the bearer token is attached to catalog calls, and the cached
 * token is reused on the second call.
 */
class CatalogOAuthClientConfigurationTest {

    private HttpServer server;
    private String serverUrl;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();

    @BeforeEach
    void startIdentityAndCatalogServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", this::issueToken);
        server.createContext("/catalog", this::receiveCatalogRequest);
        server.start();
        serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void obtainsAttachesAndReusesAClientCredentialsToken() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("catalog")
                .clientId("volt-order")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(serverUrl + "/token")
                .build();
        InMemoryClientRegistrationRepository registrations =
                new InMemoryClientRegistrationRepository(registration);
        OAuth2AuthorizedClientService authorizedClients =
                new InMemoryOAuth2AuthorizedClientService(registrations);

        CatalogOAuthClientConfiguration configuration = new CatalogOAuthClientConfiguration();
        OAuth2AuthorizedClientManager manager =
                configuration.catalogAuthorizedClientManager(registrations, authorizedClients);
        RestClient catalogClient =
                configuration.catalogRestClient(
                        manager, authorizedClients, serverUrl, Duration.ofSeconds(1), Duration.ofSeconds(3));

        assertEquals(HttpStatus.NO_CONTENT,
                catalogClient.get().uri("/catalog").retrieve().toBodilessEntity().getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT,
                catalogClient.get().uri("/catalog").retrieve().toBodilessEntity().getStatusCode());

        assertEquals("Bearer machine-token", receivedAuthorization.get());
        assertEquals(1, tokenRequests.get(), "the unexpired access token should be reused");
    }

    private void issueToken(HttpExchange exchange) throws IOException {
        tokenRequests.incrementAndGet();
        byte[] response = """
                {"access_token":"machine-token","token_type":"Bearer","expires_in":300}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void receiveCatalogRequest(HttpExchange exchange) throws IOException {
        receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}
