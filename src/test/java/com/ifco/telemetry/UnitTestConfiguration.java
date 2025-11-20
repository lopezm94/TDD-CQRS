package com.ifco.telemetry;

import com.ifco.telemetry.event.EventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test configuration providing mocked boundary beans and JPA configuration for unit tests.
 *
 * Purpose: Isolate handler unit tests from crossing boundaries.
 * - EventPublisher is the boundary between command handlers and event handlers
 * - Mocking it prevents crossing into event handler territory
 * - Allows testing handler responsibility without cross-boundary side effects
 *
 * JPA Configuration:
 * - @EnableJpaRepositories: Enables Spring Data JPA repositories
 * - @EntityScan: Configures JPA entity scanning
 * - Required when using bean whitelisting with @SpringBootTest(classes = {...})
 *
 * Usage:
 * 1. Add @Import(UnitTestConfiguration.class) to unit test class
 * 2. Reset mocks in @BeforeEach: Mockito.reset(eventPublisher)
 *    This ensures test isolation (each test starts with clean mock state)
 *
 * See TESTING-GUIDE.md for testing strategy rationale.
 */
@TestConfiguration
@EnableJpaRepositories(basePackages = "com.ifco.telemetry.repository")
@EntityScan(basePackages = "com.ifco.telemetry.domain")
public class UnitTestConfiguration {

    /**
     * Provides a mocked EventPublisher for unit tests.
     * The @Primary annotation ensures this mock takes precedence over the real bean.
     *
     * This allows command handler tests to verify event publishing behavior
     * without actually crossing into event handler territory.
     * The command handler doesn't care HOW EventPublisher works internally (Kafka, etc).
     */
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return Mockito.mock(EventPublisher.class);
    }
}
