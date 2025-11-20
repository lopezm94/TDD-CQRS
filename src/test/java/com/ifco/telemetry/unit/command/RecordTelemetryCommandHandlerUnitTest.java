package com.ifco.telemetry.unit.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ifco.telemetry.HandlerUnitTestBase;
import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import com.ifco.telemetry.domain.Telemetry;
import com.ifco.telemetry.event.EventPublisher;
import com.ifco.telemetry.event.TelemetryRecordedEvent;
import com.ifco.telemetry.repository.TelemetryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Unit tests for RecordTelemetryCommandHandler.
 *
 * Testing Strategy:
 * - Uses @SpringBootTest(classes = {...}) to whitelist only required beans
 * - @EnableAutoConfiguration loads JPA/DataSource infrastructure automatically
 * - Extends HandlerUnitTestBase to import UnitTestConfiguration (mocked EventPublisher)
 * - Tests only this handler's responsibility: save telemetry + publish event
 * - Does NOT test event handler behavior (that's tested separately)
 * - Uses real repository implementation (behavior testing)
 * - Verifies values passed to mocked boundaries (EventPublisher)
 *
 * See ADR-002 for testing strategy rationale.
 */
@SpringBootTest(classes = { RecordTelemetryCommandHandler.class })
@EnableAutoConfiguration // Loads JPA, DataSource, Transaction infrastructure
@EnableJpaRepositories(basePackages = "com.ifco.telemetry.repository")
@EntityScan(basePackages = "com.ifco.telemetry.domain")
class RecordTelemetryCommandHandlerUnitTest extends HandlerUnitTestBase {

    @Autowired
    private RecordTelemetryCommandHandler handler;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private EventPublisher eventPublisher; // Mocked via UnitTestConfiguration

    @BeforeEach
    void clearData() {
        telemetryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save telemetry with correct values")
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

        // And - Verify event was published (boundary interaction)
        ArgumentCaptor<TelemetryRecordedEvent> captor = ArgumentCaptor.forClass(
            TelemetryRecordedEvent.class
        );
        verify(eventPublisher).publish(captor.capture());

        TelemetryRecordedEvent event = captor.getValue();
        assertThat(event.deviceId()).isEqualTo(1L);
        assertThat(event.temperature()).isEqualTo(10.0);
        assertThat(event.date()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    @DisplayName("Should save multiple telemetry records")
    void should_save_multiple_telemetry_records() {
        // Given
        List<RecordTelemetryCommand> commands = List.of(
            new RecordTelemetryCommand(
                1L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            )
        );

        // When
        commands.forEach(handler::handle);

        // Then - Both records stored (append-only)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(2);
        assertThat(telemetries)
            .extracting(Telemetry::getTemperature)
            .containsExactlyInAnyOrder(10.0, 12.0);

        // And - Events published for each
        verify(eventPublisher, times(2)).publish(
            any(TelemetryRecordedEvent.class)
        );
    }

    @Test
    @DisplayName("Should save duplicate timestamps as separate records")
    void should_save_duplicate_timestamps_as_separate_records() {
        // Given - Same timestamp, different temperatures
        List<RecordTelemetryCommand> commands = List.of(
            new RecordTelemetryCommand(
                1L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                1L,
                15.0,
                Instant.parse("2025-01-31T13:00:00Z")
            )
        );

        // When
        commands.forEach(handler::handle);

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
    @DisplayName("Should save out-of-order events as received")
    void should_save_out_of_order_events_as_received() {
        // Given - Events arriving out of chronological order
        List<RecordTelemetryCommand> commands = List.of(
            new RecordTelemetryCommand(
                1L,
                20.0,
                Instant.parse("2025-01-31T13:00:20Z")
            ),
            new RecordTelemetryCommand(
                1L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                1L,
                15.0,
                Instant.parse("2025-01-31T13:00:10Z")
            )
        );

        // When - Receive out of order: future, past, middle
        commands.forEach(handler::handle);

        // Then - All stored regardless of order (append-only)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(3);
        assertThat(telemetries)
            .extracting(Telemetry::getTemperature)
            .containsExactlyInAnyOrder(20.0, 10.0, 15.0);
    }
}
