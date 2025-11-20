package com.ifco.telemetry.unit.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifco.telemetry.TestContainersBase;
import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import com.ifco.telemetry.domain.Telemetry;
import com.ifco.telemetry.repository.TelemetryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit tests for RecordTelemetryCommandHandler.
 * Uses real implementations to test behavior, not implementation details.
 * See ADR-002 for testing strategy rationale.
 */
@SpringBootTest
class RecordTelemetryCommandHandlerTest extends TestContainersBase {

    @Autowired
    private RecordTelemetryCommandHandler handler;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @BeforeEach
    void clearData() {
        telemetryRepository.deleteAll();
    }

    @Test
    void should_save_telemetry_with_correct_values() {
        // Given
        RecordTelemetryCommand command = new RecordTelemetryCommand(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );

        // When
        handler.handle(command);

        // Then - Verify behavior: telemetry is stored
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(1);
        assertThat(telemetries.get(0).getDeviceId()).isEqualTo(1L);
        assertThat(telemetries.get(0).getTemperature()).isEqualTo(10.0);
        assertThat(telemetries.get(0).getTimestamp()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    void should_save_multiple_telemetry_records() {
        // Given
        RecordTelemetryCommand command1 = new RecordTelemetryCommand(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        RecordTelemetryCommand command2 = new RecordTelemetryCommand(
            1L,
            12.0,
            Instant.parse("2025-01-31T13:00:05Z")
        );

        // When
        handler.handle(command1);
        handler.handle(command2);

        // Then - Both records stored (append-only)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
        assertThat(telemetries)
            .extracting(Telemetry::getTemperature)
            .containsExactlyInAnyOrder(10.0, 12.0);
    }

    @Test
    void should_save_duplicate_timestamps_as_separate_records() {
        // Given - Same timestamp, different temperatures
        RecordTelemetryCommand command1 = new RecordTelemetryCommand(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        RecordTelemetryCommand command2 = new RecordTelemetryCommand(
            1L,
            15.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );

        // When
        handler.handle(command1);
        handler.handle(command2);

        // Then - Both stored (append-only, no deduplication)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
        assertThat(telemetries)
            .extracting(Telemetry::getTemperature)
            .containsExactlyInAnyOrder(10.0, 15.0);
        assertThat(telemetries).allMatch(t ->
            t.getTimestamp().equals(Instant.parse("2025-01-31T13:00:00Z"))
        );
    }

    @Test
    void should_save_out_of_order_events_as_received() {
        // Given - Events arriving out of chronological order
        RecordTelemetryCommand future = new RecordTelemetryCommand(
            1L,
            20.0,
            Instant.parse("2025-01-31T13:00:20Z")
        );
        RecordTelemetryCommand past = new RecordTelemetryCommand(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        RecordTelemetryCommand middle = new RecordTelemetryCommand(
            1L,
            15.0,
            Instant.parse("2025-01-31T13:00:10Z")
        );

        // When - Receive out of order: future, past, middle
        handler.handle(future);
        handler.handle(past);
        handler.handle(middle);

        // Then - All stored regardless of order (append-only)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(3);
        assertThat(telemetries)
            .extracting(Telemetry::getTemperature)
            .containsExactlyInAnyOrder(20.0, 10.0, 15.0);
    }
}
