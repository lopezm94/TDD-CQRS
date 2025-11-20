package com.ifco.telemetry.repository;

import com.ifco.telemetry.projection.DeviceProjection;

import java.util.Optional;

/**
 * Abstraction for device projection storage.
 *
 * TDD Approach:
 * - Iteration 1: Implemented with in-memory Map (InMemoryProjectionRepository)
 * - Iteration 4: Swap to Redis implementation (tests remain unchanged)
 *
 * This interface decouples tests from infrastructure, allowing us to follow
 * true TDD progression from simple to complex implementations.
 */
public interface ProjectionRepository {

    Optional<DeviceProjection> findById(Long deviceId);

    void save(DeviceProjection projection);

    Iterable<DeviceProjection> findAll();

    void deleteAll();
}
