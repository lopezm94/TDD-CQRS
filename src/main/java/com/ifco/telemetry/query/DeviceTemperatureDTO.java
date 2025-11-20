package com.ifco.telemetry.query;

import java.time.Instant;

public record DeviceTemperatureDTO(
    Long deviceId,
    Double measurement,
    Instant date
) {}
