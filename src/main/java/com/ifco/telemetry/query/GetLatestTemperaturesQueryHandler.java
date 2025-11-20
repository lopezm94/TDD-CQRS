package com.ifco.telemetry.query;

import com.ifco.telemetry.repository.ProjectionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Returns latest temperature reading for all devices.
 *
 * @RequiredArgsConstructor - Generates constructor with final fields for dependency injection
 */
@Component
@RequiredArgsConstructor
public class GetLatestTemperaturesQueryHandler {

    private final ProjectionRepository projectionRepository;

    public List<DeviceTemperatureDTO> handle() {
        List<DeviceTemperatureDTO> results = new ArrayList<>();

        projectionRepository
            .findAll()
            .forEach(projection ->
                results.add(
                    new DeviceTemperatureDTO(
                        projection.getDeviceId(),
                        projection.getLastMeasurement(),
                        projection.getLastUpdated()
                    )
                )
            );

        return results;
    }
}
