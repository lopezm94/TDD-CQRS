package com.ifco.telemetry.command;

import com.ifco.telemetry.domain.Telemetry;
import com.ifco.telemetry.event.EventPublisher;
import com.ifco.telemetry.event.TelemetryRecordedEvent;
import com.ifco.telemetry.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles recording telemetry measurements.
 * Saves to write model and publishes event for projection updates.
 *
 * @RequiredArgsConstructor - Generates constructor with final fields for dependency injection
 * @Slf4j - Provides 'log' field for logging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordTelemetryCommandHandler {

    private final TelemetryRepository telemetryRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(RecordTelemetryCommand command) {
        // Validate inputs - fail fast with clear error messages
        if (command.deviceId() == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }
        if (command.temperature() == null) {
            throw new IllegalArgumentException("temperature cannot be null");
        }
        if (command.date() == null) {
            throw new IllegalArgumentException("date cannot be null");
        }

        // Save to write model (append-only)
        Telemetry telemetry = new Telemetry(
            command.deviceId(),
            command.temperature(),
            command.date()
        );

        Telemetry saved = telemetryRepository.save(telemetry);
        log.debug(
            "Saved telemetry id={} for device={}",
            saved.getId(),
            command.deviceId()
        );

        // Publish event (handler will update projection)
        eventPublisher.publish(
            new TelemetryRecordedEvent(
                command.deviceId(),
                command.temperature(),
                command.date()
            )
        );
    }
}
