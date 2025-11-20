package com.ifco.telemetry.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifco.telemetry.TestContainersBase;
import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import com.ifco.telemetry.repository.ProjectionRepository;
import com.ifco.telemetry.repository.TelemetryRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptance tests for error scenarios and edge cases.
 *
 * Tests system behavior under failure conditions:
 * - Invalid input validation
 * - Null handling
 * - Data constraints
 *
 * Note: Does NOT use @Transactional annotation.
 * Acceptance tests should test real transaction boundaries, not artificial ones.
 * Explicit cleanup in @BeforeEach ensures test isolation.
 */
@SpringBootTest
class TelemetryErrorScenariosAcceptanceTest extends TestContainersBase {

    @Autowired
    private RecordTelemetryCommandHandler commandHandler;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private ProjectionRepository projectionRepository;

    @BeforeEach
    void clearData() {
        // Explicit cleanup before each test for isolation
        telemetryRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should reject command with null device ID")
    void should_reject_null_device_id() {
        // Given
        var command = new RecordTelemetryCommand(null, 25.0, Instant.now());

        // When/Then
        assertThatThrownBy(() -> commandHandler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("deviceId cannot be null");

        // Verify no data was persisted
        assertThat(telemetryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should reject command with null temperature")
    void should_reject_null_temperature() {
        // Given
        var command = new RecordTelemetryCommand(1L, null, Instant.now());

        // When/Then
        assertThatThrownBy(() -> commandHandler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("temperature cannot be null");

        // Verify no data was persisted
        assertThat(telemetryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should reject command with null timestamp")
    void should_reject_null_timestamp() {
        // Given
        var command = new RecordTelemetryCommand(1L, 25.0, null);

        // When/Then
        assertThatThrownBy(() -> commandHandler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("date cannot be null");

        // Verify no data was persisted
        assertThat(telemetryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should handle extreme temperature values")
    void should_handle_extreme_temperatures() {
        // Given - Extreme but valid values
        var coldCommand = new RecordTelemetryCommand(
            1L,
            -273.15,
            Instant.now()
        ); // Absolute zero
        var hotCommand = new RecordTelemetryCommand(2L, 1000.0, Instant.now()); // Very hot

        // When
        commandHandler.handle(coldCommand);
        commandHandler.handle(hotCommand);

        // Then - Should be stored successfully
        var telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
        assertThat(telemetries)
            .extracting("temperature")
            .containsExactlyInAnyOrder(-273.15, 1000.0);
    }

    @Test
    @DisplayName("Should handle very old and future timestamps")
    void should_handle_extreme_timestamps() {
        // Given
        var veryOld = new RecordTelemetryCommand(
            1L,
            20.0,
            Instant.parse("1970-01-01T00:00:00Z")
        );
        var farFuture = new RecordTelemetryCommand(
            2L,
            25.0,
            Instant.parse("2099-12-31T23:59:59Z")
        );

        // When
        commandHandler.handle(veryOld);
        commandHandler.handle(farFuture);

        // Then
        var telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
    }

    @Test
    @DisplayName("Should handle device ID boundary values")
    void should_handle_device_id_boundaries() {
        // Given - Min and max Long values
        var minDevice = new RecordTelemetryCommand(1L, 20.0, Instant.now());
        var maxDevice = new RecordTelemetryCommand(
            Long.MAX_VALUE,
            25.0,
            Instant.now()
        );

        // When
        commandHandler.handle(minDevice);
        commandHandler.handle(maxDevice);

        // Then
        var telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
        assertThat(telemetries)
            .extracting("deviceId")
            .containsExactlyInAnyOrder(1L, Long.MAX_VALUE);
    }

    @Test
    @DisplayName("Should handle rapid succession of commands for same device")
    void should_handle_rapid_commands() {
        // Given - Many commands in rapid succession
        var baseTime = Instant.now();

        // When - Submit 100 commands rapidly
        for (int i = 0; i < 100; i++) {
            var command = new RecordTelemetryCommand(
                1L,
                20.0 + i * 0.1,
                baseTime.plusMillis(i)
            );
            commandHandler.handle(command);
        }

        // Then - All should be persisted
        var telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(100);
    }
}
