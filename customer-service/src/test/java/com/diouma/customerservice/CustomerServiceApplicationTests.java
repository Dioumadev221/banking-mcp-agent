package com.diouma.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Loads the full context against a real PostgreSQL started in a container, so
 * the Flyway migrations and the entity mapping are exercised on the database
 * the service actually runs on - not a stand-in that would hide dialect gaps.
 *
 * <p>{@code @ServiceConnection} wires the container straight into the
 * datasource, so there is no JDBC URL to repeat here. Eureka is turned off:
 * this test has nothing to register with.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@Testcontainers
class CustomerServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void contextLoads() {
    }

}
