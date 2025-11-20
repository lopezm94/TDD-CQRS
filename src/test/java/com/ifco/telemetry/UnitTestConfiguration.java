package com.ifco.telemetry;

import com.ifco.telemetry.event.EventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration providing mocked boundary beans for unit tests.
 *
 * Purpose: Isolate handler unit tests from crossing boundaries.
 * - EventPublisher is a boundary to the event system
 * - Mocking it prevents event handlers from being triggered
 * - Allows testing handler responsibility without cross-boundary side effects
 *
 * Usage:
 * 1. Add @Import(UnitTestConfiguration.class) to unit test class
 * 2. Reset mocks in @BeforeEach: Mockito.reset(eventPublisher)
 *    This ensures test isolation (each test starts with clean mock state)
 *
 * See ADR-002 for testing strategy rationale.
 */
@TestConfiguration
public class UnitTestConfiguration {

    /**
     * Provides a mocked EventPublisher for unit tests.
     * The @Primary annotation ensures this mock takes precedence over the real bean.
     *
     * This allows command handler tests to verify event publishing behavior
     * without actually triggering event handlers and crossing test boundaries.
     */
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return Mockito.mock(EventPublisher.class);
    }
}
