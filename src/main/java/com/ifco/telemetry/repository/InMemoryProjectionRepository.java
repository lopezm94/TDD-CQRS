package com.ifco.telemetry.repository;

import com.ifco.telemetry.projection.DeviceProjection;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ProjectionRepository for Iteration 1.
 *
 * Uses ConcurrentHashMap for thread-safety.
 *
 * Will be replaced with RedisProjectionRepository in Iteration 4,
 * but all tests will continue to work unchanged due to the interface abstraction.
 */
@Repository
public class InMemoryProjectionRepository implements ProjectionRepository {

    private final ConcurrentHashMap<Long, DeviceProjection> store = new ConcurrentHashMap<>();

    @Override
    public Optional<DeviceProjection> findById(Long deviceId) {
        return Optional.ofNullable(store.get(deviceId));
    }

    @Override
    public void save(DeviceProjection projection) {
        store.put(projection.getDeviceId(), projection);
    }

    @Override
    public Iterable<DeviceProjection> findAll() {
        return store.values();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
