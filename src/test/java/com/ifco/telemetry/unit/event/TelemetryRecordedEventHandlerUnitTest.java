package com.ifco.telemetry.unit.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifco.telemetry.TestContainersBase;
import com.ifco.telemetry.UnitTestConfiguration;
import com.ifco.telemetry.event.TelemetryRecordedEvent;
import com.ifco.telemetry.event.TelemetryRecordedEventHandler;
import com.ifco.telemetry.projection.DeviceProjection;
import com.ifco.telemetry.repository.ProjectionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(UnitTestConfiguration.class)
class TelemetryRecordedEventHandlerUnitTest extends TestContainersBase {

    @Autowired
    private TelemetryRecordedEventHandler eventHandler;

    @Autowired
    private ProjectionRepository projectionRepository;

    @BeforeEach
    void clearData() {
        projectionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create projection for first telemetry event")
    void should_create_projection_for_first_telemetry() {
        // Given
        TelemetryRecordedEvent event = new TelemetryRecordedEvent(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );

        // When
        eventHandler.handle(event);

        // Then
        Optional<DeviceProjection> projection = projectionRepository.findById(
            1L
        );
        assertThat(projection).isPresent();
        assertThat(projection.get().getDeviceId()).isEqualTo(1L);
        assertThat(projection.get().getLastMeasurement()).isEqualTo(10.0);
        assertThat(projection.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    @DisplayName("Should update projection when newer event arrives")
    void should_update_projection_when_newer_event_arrives() {
        // Given - Initial projection
        DeviceProjection initial = new DeviceProjection(
            1L,
            8.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        projectionRepository.save(initial);

        // When - Newer event arrives
        TelemetryRecordedEvent newerEvent = new TelemetryRecordedEvent(
            1L,
            12.0,
            Instant.parse("2025-01-31T13:00:05Z")
        );
        eventHandler.handle(newerEvent);

        // Then - Projection should be updated
        Optional<DeviceProjection> projection = projectionRepository.findById(
            1L
        );
        assertThat(projection).isPresent();
        assertThat(projection.get().getLastMeasurement()).isEqualTo(12.0);
        assertThat(projection.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:05Z")
        );
    }

    @Test
    @DisplayName(
        "Should NOT update projection when older event arrives (out-of-order)"
    )
    void should_not_update_projection_when_older_event_arrives() {
        // Given - Projection with recent data
        DeviceProjection existing = new DeviceProjection(
            1L,
            12.0,
            Instant.parse("2025-01-31T13:00:05Z")
        );
        projectionRepository.save(existing);

        // When - Older event arrives
        TelemetryRecordedEvent olderEvent = new TelemetryRecordedEvent(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        eventHandler.handle(olderEvent);

        // Then - Projection should remain unchanged
        Optional<DeviceProjection> projection = projectionRepository.findById(
            1L
        );
        assertThat(projection).isPresent();
        assertThat(projection.get().getLastMeasurement()).isEqualTo(12.0);
        assertThat(projection.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:05Z")
        );
    }

    @Test
    @DisplayName("Should handle exact duplicate idempotently")
    void should_handle_exact_duplicate_idempotently() {
        // Given - Projection exists
        DeviceProjection existing = new DeviceProjection(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        projectionRepository.save(existing);

        // When - Exact duplicate event
        TelemetryRecordedEvent duplicateEvent = new TelemetryRecordedEvent(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        eventHandler.handle(duplicateEvent);

        // Then - Projection unchanged (idempotent)
        Optional<DeviceProjection> projection = projectionRepository.findById(
            1L
        );
        assertThat(projection).isPresent();
        assertThat(projection.get().getLastMeasurement()).isEqualTo(10.0);
        assertThat(projection.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    @DisplayName("Should update when same timestamp but different temperature")
    void should_update_when_same_timestamp_different_temperature() {
        // Given - Projection exists
        DeviceProjection existing = new DeviceProjection(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        projectionRepository.save(existing);

        // When - Same timestamp, different temperature
        TelemetryRecordedEvent conflictingEvent = new TelemetryRecordedEvent(
            1L,
            15.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        eventHandler.handle(conflictingEvent);

        // Then - Last processed wins
        Optional<DeviceProjection> projection = projectionRepository.findById(
            1L
        );
        assertThat(projection).isPresent();
        assertThat(projection.get().getLastMeasurement()).isEqualTo(15.0);
        assertThat(projection.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    @DisplayName("Should handle rapid succession of events correctly")
    void should_handle_rapid_succession_of_events() {
        // Given - Initial projection
        DeviceProjection projection = new DeviceProjection(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        projectionRepository.save(projection);

        // When - Process multiple events in order
        java.util.List.of(
            new TelemetryRecordedEvent(
                1L,
                11.0,
                Instant.parse("2025-01-31T13:00:01Z")
            ),
            new TelemetryRecordedEvent(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:02Z")
            ),
            new TelemetryRecordedEvent(
                1L,
                13.0,
                Instant.parse("2025-01-31T13:00:03Z")
            )
        ).forEach(eventHandler::handle);

        // Then - Final state should be the latest
        Optional<DeviceProjection> result = projectionRepository.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getLastMeasurement()).isEqualTo(13.0);
        assertThat(result.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:03Z")
        );
    }

    @Test
    @DisplayName("Should maintain latest value with out-of-order events")
    void should_maintain_latest_value_with_out_of_order_events() {
        // Given - Initial projection
        DeviceProjection projection = new DeviceProjection(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );
        projectionRepository.save(projection);

        // When - Process events out of order
        java.util.List.of(
            new TelemetryRecordedEvent(
                1L,
                15.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ), // Future
            new TelemetryRecordedEvent(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:02Z")
            ), // Past (ignored)
            new TelemetryRecordedEvent(
                1L,
                20.0,
                Instant.parse("2025-01-31T13:00:10Z")
            ), // More future
            new TelemetryRecordedEvent(
                1L,
                11.0,
                Instant.parse("2025-01-31T13:00:01Z")
            ) // Past (ignored)
        ).forEach(eventHandler::handle);

        // Then - Should keep the chronologically latest
        Optional<DeviceProjection> result = projectionRepository.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getLastMeasurement()).isEqualTo(20.0);
        assertThat(result.get().getLastUpdated()).isEqualTo(
            Instant.parse("2025-01-31T13:00:10Z")
        );
    }
}
