package com.ifco.telemetry.event;

import com.ifco.telemetry.projection.DeviceProjection;
import com.ifco.telemetry.repository.ProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Handles TelemetryRecordedEvent to update read projections.
 * Consumes events from Kafka topic "telemetry.events".
 *
 * @RequiredArgsConstructor - Generates constructor with final fields for dependency injection
 * @Slf4j - Provides 'log' field for logging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryRecordedEventHandler {

    private final ProjectionRepository projectionRepository;

    @KafkaListener(
        topics = "telemetry.events",
        groupId = "telemetry-consumer-group"
    )
    public void handle(TelemetryRecordedEvent event) {
        var projection = projectionRepository
            .findById(event.deviceId())
            .orElse(new DeviceProjection(event.deviceId(), null, null));

        // Update if this is the latest or first measurement
        if (
            projection.getLastUpdated() == null ||
            event.date().isAfter(projection.getLastUpdated()) ||
            event.date().equals(projection.getLastUpdated())
        ) {
            projection.setLastTemperature(event.temperature());
            projection.setLastUpdated(event.date());
            projectionRepository.save(projection);
            log.debug("Updated projection for device={}", event.deviceId());
        } else {
            log.debug(
                "Ignored older event for device={}: event time {} < projection time {}",
                event.deviceId(),
                event.date(),
                projection.getLastUpdated()
            );
        }
    }
}
