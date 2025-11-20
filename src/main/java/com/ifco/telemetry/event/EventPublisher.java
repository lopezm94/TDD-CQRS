package com.ifco.telemetry.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events.
 * Currently synchronous in-memory delivery. Will use Kafka for async messaging.
 *
 * @RequiredArgsConstructor - Generates constructor with final fields for dependency injection
 * @Slf4j - Provides 'log' field for logging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final TelemetryRecordedEventHandler eventHandler;

    public void publish(TelemetryRecordedEvent event) {
        log.debug(
            "Publishing event: deviceId={}, temperature={}, date={}",
            event.deviceId(),
            event.temperature(),
            event.date()
        );

        // Synchronous event delivery for now
        eventHandler.handle(event);
    }
}
