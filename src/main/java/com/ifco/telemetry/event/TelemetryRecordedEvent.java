package com.ifco.telemetry.event;

import java.time.Instant;

public record TelemetryRecordedEvent(
        Long deviceId,
        Double temperature,
        Instant date
) {}
