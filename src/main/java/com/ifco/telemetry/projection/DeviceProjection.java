package com.ifco.telemetry.projection;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection for device's latest temperature reading.
 *
 * Uses regular class (not record) because:
 * - Needs to be mutable (updated when new events arrive)
 * - Requires no-arg constructor for framework instantiation (Jackson, Redis, etc.)
 *
 * Lombok annotations explained:
 * @NoArgsConstructor - Required by frameworks that use reflection to instantiate objects
 * @AllArgsConstructor - Convenient constructor for creating instances in code
 * @Data - Generates getters, setters, equals, hashCode, toString
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceProjection {

    private Long deviceId;
    private Double lastTemperature;
    private Instant lastUpdated;
}
