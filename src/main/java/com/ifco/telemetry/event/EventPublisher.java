package com.ifco.telemetry.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka.
 * Events are sent to the "telemetry.events" topic for async processing.
 *
 * @RequiredArgsConstructor - Generates constructor with final fields for dependency injection
 * @Slf4j - Provides 'log' field for logging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "telemetry.events";

    public void publish(TelemetryRecordedEvent event) {
        log.debug(
            "Publishing event to Kafka: deviceId={}, temperature={}, date={}",
            event.deviceId(),
            event.temperature(),
            event.date()
        );

        kafkaTemplate.send(TOPIC, event);
    }
}
