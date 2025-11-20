package com.ifco.telemetry.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ifco.telemetry.TestContainersBase;
import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import com.ifco.telemetry.domain.Telemetry;
import com.ifco.telemetry.projection.DeviceProjection;
import com.ifco.telemetry.query.DeviceTemperatureDTO;
import com.ifco.telemetry.query.GetLatestTemperaturesQueryHandler;
import com.ifco.telemetry.repository.ProjectionRepository;
import com.ifco.telemetry.repository.TelemetryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TelemetryFlowAcceptanceTest extends TestContainersBase {

    @Autowired
    private RecordTelemetryCommandHandler commandHandler;

    @Autowired
    private GetLatestTemperaturesQueryHandler queryHandler;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private ProjectionRepository projectionRepository;

    @BeforeEach
    void clearData() {
        telemetryRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void should_record_and_retrieve_single_device_telemetry() {
        // Given - Record telemetry
        RecordTelemetryCommand command = new RecordTelemetryCommand(
            1L,
            10.0,
            Instant.parse("2025-01-31T13:00:00Z")
        );

        // When - Execute command
        commandHandler.handle(command);

        // Then - Telemetry should be stored
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(1);
        assertThat(telemetries.get(0).getDeviceId()).isEqualTo(1L);
        assertThat(telemetries.get(0).getTemperature()).isEqualTo(10.0);

        // And - Wait for projection to be updated
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                // Projection should be updated via event
                Optional<DeviceProjection> projection =
                    projectionRepository.findById(1L);
                assertThat(projection).isPresent();
                assertThat(projection.get().getLastTemperature()).isEqualTo(
                    10.0
                );
            });

        // And - Query should return the latest temperature
        List<DeviceTemperatureDTO> results = queryHandler.handle();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).deviceId()).isEqualTo(1L);
        assertThat(results.get(0).temperature()).isEqualTo(10.0);
    }

    @Test
    void should_handle_multiple_devices_with_multiple_measurements() {
        // Given - Multiple measurements for multiple devices
        List<RecordTelemetryCommand> commands = List.of(
            // Device 1: 3 measurements
            new RecordTelemetryCommand(
                1L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ),
            new RecordTelemetryCommand(
                1L,
                11.5,
                Instant.parse("2025-01-31T13:00:10Z")
            ),
            // Device 2: 4 measurements
            new RecordTelemetryCommand(
                2L,
                8.0,
                Instant.parse("2025-01-31T13:00:01Z")
            ),
            new RecordTelemetryCommand(
                2L,
                19.0,
                Instant.parse("2025-01-31T13:00:06Z")
            ),
            new RecordTelemetryCommand(
                2L,
                10.0,
                Instant.parse("2025-01-31T13:00:11Z")
            ),
            new RecordTelemetryCommand(
                2L,
                9.5,
                Instant.parse("2025-01-31T13:00:16Z")
            ),
            // Device 3: 2 measurements
            new RecordTelemetryCommand(
                3L,
                15.0,
                Instant.parse("2025-01-31T13:00:02Z")
            ),
            new RecordTelemetryCommand(
                3L,
                16.0,
                Instant.parse("2025-01-31T13:00:07Z")
            )
        );

        // When
        commands.forEach(commandHandler::handle);

        // Then - All telemetry should be stored
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(9);

        // And - Wait for all projections to be updated
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<DeviceTemperatureDTO> results = queryHandler.handle();
                assertThat(results).hasSize(3);

                // Device 1: latest should be 11.5°C at 13:00:10
                DeviceTemperatureDTO device1 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(1L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device1.temperature()).isEqualTo(11.5);
                assertThat(device1.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:10Z")
                );

                // Device 2: latest should be 9.5°C at 13:00:16
                DeviceTemperatureDTO device2 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(2L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device2.temperature()).isEqualTo(9.5);
                assertThat(device2.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:16Z")
                );

                // Device 3: latest should be 16.0°C at 13:00:07
                DeviceTemperatureDTO device3 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(3L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device3.temperature()).isEqualTo(16.0);
                assertThat(device3.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:07Z")
                );
            });
    }

    @Test
    void should_handle_out_of_order_and_duplicate_events() {
        // Given - Edge case scenarios
        List<RecordTelemetryCommand> commands = List.of(
            // Device 1: Out-of-order events
            new RecordTelemetryCommand(
                1L,
                15.0,
                Instant.parse("2025-01-31T13:00:10Z")
            ), // Future
            new RecordTelemetryCommand(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ), // Past (ignored)
            new RecordTelemetryCommand(
                1L,
                20.0,
                Instant.parse("2025-01-31T13:00:20Z")
            ), // More future
            new RecordTelemetryCommand(
                1L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ), // Past (ignored)
            // Device 2: Duplicate events
            new RecordTelemetryCommand(
                2L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                2L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ), // Exact duplicate
            new RecordTelemetryCommand(
                2L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ),
            new RecordTelemetryCommand(
                2L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ), // Exact duplicate
            // Device 3: Same timestamp, different values (conflict)
            new RecordTelemetryCommand(
                3L,
                10.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ),
            new RecordTelemetryCommand(
                3L,
                15.0,
                Instant.parse("2025-01-31T13:00:00Z")
            ), // Same timestamp, different temp (last wins)
            new RecordTelemetryCommand(
                3L,
                20.0,
                Instant.parse("2025-01-31T13:00:05Z")
            )
        );

        // When
        commands.forEach(commandHandler::handle);

        // Then - All events stored (append-only)
        List<Telemetry> telemetries = telemetryRepository.findAll();
        assertThat(telemetries).hasSize(11); // All events persisted

        // And - Wait for projections to reflect conflict resolution
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<DeviceTemperatureDTO> results = queryHandler.handle();
                assertThat(results).hasSize(3);

                // Device 1: Should keep chronologically latest (20.0°C at 13:00:20)
                DeviceTemperatureDTO device1 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(1L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device1.temperature()).isEqualTo(20.0);
                assertThat(device1.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:20Z")
                );

                // Device 2: Duplicates are idempotent (12.0°C at 13:00:05)
                DeviceTemperatureDTO device2 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(2L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device2.temperature()).isEqualTo(12.0);
                assertThat(device2.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:05Z")
                );

                // Device 3: Conflict resolution - last processed wins for same timestamp (20.0°C at 13:00:05)
                DeviceTemperatureDTO device3 = results
                    .stream()
                    .filter(dto -> dto.deviceId().equals(3L))
                    .findFirst()
                    .orElseThrow();
                assertThat(device3.temperature()).isEqualTo(20.0);
                assertThat(device3.date()).isEqualTo(
                    Instant.parse("2025-01-31T13:00:05Z")
                );
            });
    }
}
