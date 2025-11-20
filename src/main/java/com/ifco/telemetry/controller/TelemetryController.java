package com.ifco.telemetry.controller;

import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for telemetry recording operations.
 * Entry point for command side of CQRS pattern.
 */
@RestController
@RequestMapping("/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final RecordTelemetryCommandHandler commandHandler;

    /**
     * Records telemetry data from a device.
     * Transforms HTTP request to command and delegates to handler.
     *
     * @param request the telemetry data to record
     * @return 202 Accepted if command was successfully processed
     */
    @PostMapping
    public ResponseEntity<Void> recordTelemetry(@RequestBody @Valid TelemetryRequest request) {
        RecordTelemetryCommand command = new RecordTelemetryCommand(
            request.deviceId(),
            request.measurement(),
            request.date()
        );
        commandHandler.handle(command);
        return ResponseEntity.accepted().build();
    }
}
