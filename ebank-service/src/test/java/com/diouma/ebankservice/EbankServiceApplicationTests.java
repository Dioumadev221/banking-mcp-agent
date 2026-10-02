package com.diouma.ebankservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole service against a real PostgreSQL, started in a throw-away
 * container. This exercises what H2 never could: that the Flyway migration
 * applies cleanly and that Hibernate's schema validation agrees with it. If an
 * entity and its migration drift apart, this test fails.
 *
 * <p>Eureka and the startup seeding are switched off: neither belongs to the
 * question being asked here (does the schema load?), and the seeding would
 * otherwise reach out to customer-service, which is not running in a unit build.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "demo.seed-accounts=false"
})
@Testcontainers
class EbankServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void contextLoads() {
    }

}
