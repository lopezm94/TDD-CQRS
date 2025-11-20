package com.ifco.telemetry.controller;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * HTTP request DTO for recording telemetry data.
 * Immutable record that maps to RecordTelemetryCommand.
 */
public record TelemetryRequest(
    @NotNull(message = "Device ID is required")
    Long deviceId,

    @NotNull(message = "Measurement is required")
    Double measurement,

    @NotNull(message = "Date is required")
    Instant date
) {}
