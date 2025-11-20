package com.ifco.telemetry.controller;

import com.ifco.telemetry.query.DeviceTemperatureDTO;
import com.ifco.telemetry.query.GetLatestTemperaturesQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for device query operations.
 * Entry point for query side of CQRS pattern.
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final GetLatestTemperaturesQueryHandler queryHandler;

    /**
     * Retrieves the latest temperature for all devices.
     * Delegates to query handler and returns results as JSON.
     *
     * @return list of device temperatures (empty list if no devices)
     */
    @GetMapping("/temperatures")
    public List<DeviceTemperatureDTO> getLatestTemperatures() {
        return queryHandler.handle();
    }
}
