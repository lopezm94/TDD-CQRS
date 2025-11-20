package com.ifco.telemetry.unit.entrypoint.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ifco.telemetry.controller.DeviceController;
import com.ifco.telemetry.query.DeviceTemperatureDTO;
import com.ifco.telemetry.query.GetLatestTemperaturesQueryHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for DeviceController.
 * Tests the HTTP layer in isolation by mocking the query handler.
 * Verifies correct response formatting and status codes.
 */
@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @MockBean
    private GetLatestTemperaturesQueryHandler queryHandler;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return latest temperatures from query handler")
    void should_return_latest_temperatures() throws Exception {
        // Given
        List<DeviceTemperatureDTO> expectedResults = List.of(
            new DeviceTemperatureDTO(
                1L,
                12.0,
                Instant.parse("2025-01-31T13:00:05Z")
            ),
            new DeviceTemperatureDTO(
                2L,
                10.0,
                Instant.parse("2025-01-31T13:00:11Z")
            )
        );
        when(queryHandler.handle()).thenReturn(expectedResults);

        // When & Then
        mockMvc
            .perform(get("/devices/temperatures"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].deviceId").value(1))
            .andExpect(jsonPath("$[0].measurement").value(12.0))
            .andExpect(jsonPath("$[0].date").value("2025-01-31T13:00:05Z"))
            .andExpect(jsonPath("$[1].deviceId").value(2))
            .andExpect(jsonPath("$[1].measurement").value(10.0))
            .andExpect(jsonPath("$[1].date").value("2025-01-31T13:00:11Z"));

        verify(queryHandler).handle();
    }

    @Test
    @DisplayName("Should return empty list when no devices")
    void should_return_empty_list_when_no_devices() throws Exception {
        // Given
        when(queryHandler.handle()).thenReturn(List.of());

        // When & Then
        mockMvc
            .perform(get("/devices/temperatures"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        verify(queryHandler).handle();
    }
}
