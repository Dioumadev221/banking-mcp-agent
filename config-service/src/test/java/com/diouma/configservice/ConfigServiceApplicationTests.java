package com.diouma.configservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the served configuration agrees with the defaults each service
 * ships.
 *
 * <p>The platform must behave the same whether or not the config server runs.
 * If the two ever diverge, the stack becomes reproducible only for whoever
 * happens to have the config server up - precisely the failure this repository
 * is meant to avoid.
 *
 * <p>Queried with the JDK HTTP client rather than a Spring test client: it
 * keeps this module free of a test-only HTTP dependency, and the config server
 * is consumed over plain HTTP anyway.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class ConfigServiceApplicationTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void serves_customer_service_settings() throws Exception {
        // The datasource is deliberately not centralised: a connection string is
        // environment specific (and a secret in production), so it stays with the
        // service and is set through DB_* variables. The config server carries the
        // port, which is the same everywhere.
        assertThat(get("/customer-service/default")).contains("8056");
    }

    @Test
    void serves_ebank_service_settings_including_the_mcp_server() throws Exception {
        String body = get("/ebank-service/default");

        assertThat(body).contains("8057");
        assertThat(body).contains("STREAMABLE");
    }

    @Test
    void serves_gateway_service_settings() throws Exception {
        assertThat(get("/gateway-service/default")).contains("8058");
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        }
    }
}
