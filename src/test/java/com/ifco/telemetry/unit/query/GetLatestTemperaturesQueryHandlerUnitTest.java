package com.ifco.telemetry.unit.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifco.telemetry.TestContainersBase;
import com.ifco.telemetry.UnitTestConfiguration;
import com.ifco.telemetry.projection.DeviceProjection;
import com.ifco.telemetry.query.DeviceTemperatureDTO;
import com.ifco.telemetry.query.GetLatestTemperaturesQueryHandler;
import com.ifco.telemetry.repository.ProjectionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(UnitTestConfiguration.class)
class GetLatestTemperaturesQueryHandlerUnitTest extends TestContainersBase {

    @Autowired
    private GetLatestTemperaturesQueryHandler queryHandler;

    @Autowired
    private ProjectionRepository projectionRepository;

    @BeforeEach
    void clearData() {
        projectionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return all device projections as DTOs")
    void should_return_all_device_projections() {
        // Given - Multiple projections
        List.of(
            new DeviceProjection(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ),
            new DeviceProjection(
                2L,
                10.0,
                Instant.parse("2025-01-31T13:00:11Z")
            )
        ).forEach(projectionRepository::save);

        // When
        List<DeviceTemperatureDTO> results = queryHandler.handle();

        // Then
        assertThat(results).hasSize(2);

        DeviceTemperatureDTO device1 = results
            .stream()
            .filter(dto -> dto.deviceId().equals(1L))
            .findFirst()
            .orElseThrow();
        assertThat(device1.measurement()).isEqualTo(12.0);
        assertThat(device1.date()).isEqualTo(
            Instant.parse("2025-01-31T13:00:05Z")
        );

        DeviceTemperatureDTO device2 = results
            .stream()
            .filter(dto -> dto.deviceId().equals(2L))
            .findFirst()
            .orElseThrow();
        assertThat(device2.measurement()).isEqualTo(10.0);
        assertThat(device2.date()).isEqualTo(
            Instant.parse("2025-01-31T13:00:11Z")
        );
    }

    @Test
    @DisplayName("Should return empty list when no projections exist")
    void should_return_empty_list_when_no_projections() {
        // Given - No projections

        // When
        List<DeviceTemperatureDTO> results = queryHandler.handle();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle single device projection")
    void should_handle_single_device_projection() {
        // Given - Single projection
        projectionRepository.save(
            new DeviceProjection(
                1L,
                15.5,
                Instant.parse("2025-01-31T14:30:00Z")
            )
        );

        // When
        List<DeviceTemperatureDTO> results = queryHandler.handle();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).deviceId()).isEqualTo(1L);
        assertThat(results.get(0).measurement()).isEqualTo(15.5);
        assertThat(results.get(0).date()).isEqualTo(
            Instant.parse("2025-01-31T14:30:00Z")
        );
    }
}
