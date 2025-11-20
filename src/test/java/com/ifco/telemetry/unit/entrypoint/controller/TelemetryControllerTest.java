package com.ifco.telemetry.unit.entrypoint.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ifco.telemetry.command.RecordTelemetryCommand;
import com.ifco.telemetry.command.RecordTelemetryCommandHandler;
import com.ifco.telemetry.controller.TelemetryController;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for TelemetryController.
 * Tests the HTTP layer in isolation by mocking the command handler.
 * Verifies correct request parsing, validation, and command transformation.
 */
@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @MockBean
    private RecordTelemetryCommandHandler commandHandler;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName(
        "Should accept telemetry and call handler with correct command"
    )
    void should_accept_telemetry_and_call_handler_with_correct_command()
        throws Exception {
        // Given
        String payload = """
            {
                "deviceId": 1,
                "measurement": 10.0,
                "date": "2025-01-31T13:00:00Z"
            }
            """;

        // When
        mockMvc
            .perform(
                post("/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
            .andExpect(status().isAccepted());

        // Then - Verify handler called with correct command
        ArgumentCaptor<RecordTelemetryCommand> captor = ArgumentCaptor.forClass(
            RecordTelemetryCommand.class
        );
        verify(commandHandler).handle(captor.capture());

        RecordTelemetryCommand command = captor.getValue();
        assertThat(command.deviceId()).isEqualTo(1L);
        assertThat(command.temperature()).isEqualTo(10.0);
        assertThat(command.date()).isEqualTo(
            Instant.parse("2025-01-31T13:00:00Z")
        );
    }

    @Test
    @DisplayName("Should return bad request for missing required fields")
    void should_return_bad_request_for_invalid_payload() throws Exception {
        // Given - Missing required fields
        String invalidPayload = """
            {
                "deviceId": 1
            }
            """;

        // When & Then
        mockMvc
            .perform(
                post("/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload)
            )
            .andExpect(status().isBadRequest());

        // Handler should not be called
        verify(commandHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should return bad request for null device ID")
    void should_return_bad_request_for_null_device_id() throws Exception {
        // Given
        String invalidPayload = """
            {
                "deviceId": null,
                "measurement": 10.0,
                "date": "2025-01-31T13:00:00Z"
            }
            """;

        // When & Then
        mockMvc
            .perform(
                post("/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload)
            )
            .andExpect(status().isBadRequest());

        verify(commandHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should return bad request for null measurement")
    void should_return_bad_request_for_null_measurement() throws Exception {
        // Given
        String invalidPayload = """
            {
                "deviceId": 1,
                "measurement": null,
                "date": "2025-01-31T13:00:00Z"
            }
            """;

        // When & Then
        mockMvc
            .perform(
                post("/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload)
            )
            .andExpect(status().isBadRequest());

        verify(commandHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should return bad request for null date")
    void should_return_bad_request_for_null_date() throws Exception {
        // Given
        String invalidPayload = """
            {
                "deviceId": 1,
                "measurement": 10.0,
                "date": null
            }
            """;

        // When & Then
        mockMvc
            .perform(
                post("/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload)
            )
            .andExpect(status().isBadRequest());

        verify(commandHandler, never()).handle(any());
    }
}
