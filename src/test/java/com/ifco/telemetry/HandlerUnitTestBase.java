package com.ifco.telemetry;

import com.ifco.telemetry.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Base class for handler unit tests.
 *
 * Provides:
 * - TestContainers setup (via TestContainersBase)
 * - Mocked boundary beans (via UnitTestConfiguration)
 * - Automatic mock reset before each test (test isolation)
 *
 * Usage: Extend this class for handler unit tests
 *
 * See ADR-002 for testing strategy rationale.
 */
@Import(UnitTestConfiguration.class)
public abstract class HandlerUnitTestBase extends TestContainersBase {

    @Autowired(required = false)
    private EventPublisher eventPublisher;

    /**
     * Reset all mocked boundaries before each test.
     * Ensures test isolation - each test starts with clean mock state.
     */
    @BeforeEach
    void resetMocks() {
        if (eventPublisher != null) {
            Mockito.reset(eventPublisher);
        }
    }
}
