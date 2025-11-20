package com.ifco.telemetry.command;

import java.time.Instant;

public record RecordTelemetryCommand(
        Long deviceId,
        Double temperature,
        Instant date
) {}
