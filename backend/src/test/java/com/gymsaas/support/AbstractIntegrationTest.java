package com.gymsaas.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the real application against PostgreSQL with two database roles:
 * the Flyway owner (superuser in the container) and the runtime role
 * (NOSUPERUSER / NOBYPASSRLS) so RLS is actually enforced in tests.
 *
 * <p>The container uses the singleton pattern (started once, never stopped by
 * the JUnit extension) so several IT classes can share the same database.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gymsaas")
                    .withUsername("gymsaas_owner")
                    .withPassword("gymsaas_owner_pw")
                    .withInitScript("db/test/init-app-role.sql");

    protected static final java.nio.file.Path CONTRACT_STORAGE;

    static {
        POSTGRES.start();
        try {
            CONTRACT_STORAGE = java.nio.file.Files.createTempDirectory("gymsaas-contracts-it");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @DynamicPropertySource
    static void configureDatasources(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "gymsaas_app");
        registry.add("spring.datasource.password", () -> "gymsaas_app_pw");
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", () -> "gymsaas_owner");
        registry.add("spring.flyway.password", () -> "gymsaas_owner_pw");
        registry.add("app.demo-data.enabled", () -> "true");
        // Phase 5: contract PDFs go to a throwaway directory; the hourly lifecycle
        // job is disabled so tests drive it explicitly.
        registry.add("app.storage.contracts.base-dir", () -> CONTRACT_STORAGE.toString());
        registry.add("app.memberships.lifecycle.enabled", () -> "false");
    }
}
