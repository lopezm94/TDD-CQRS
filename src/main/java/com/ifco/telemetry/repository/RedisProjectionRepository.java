package com.ifco.telemetry.repository;

import com.ifco.telemetry.projection.DeviceProjection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed implementation of ProjectionRepository.
 *
 * Stores DeviceProjection as JSON in Redis with key pattern: device:projection:{deviceId}
 *
 * Uses RedisTemplate configured with:
 * - String key serialization
 * - JSON value serialization (Jackson with JavaTimeModule for Instant support)
 *
 * Hexagonal Architecture: ProjectionRepository is a port (interface),
 * this class is an adapter that connects to Redis (external infrastructure).
 */
@Repository
public class RedisProjectionRepository implements ProjectionRepository {

    private static final String KEY_PREFIX = "device:projection:";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisProjectionRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<DeviceProjection> findById(Long deviceId) {
        String key = KEY_PREFIX + deviceId;
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value)
                .map(obj -> (DeviceProjection) obj);
    }

    @Override
    public void save(DeviceProjection projection) {
        String key = KEY_PREFIX + projection.getDeviceId();
        redisTemplate.opsForValue().set(key, projection);
    }

    @Override
    public Iterable<DeviceProjection> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }

        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(obj -> obj != null)
                .map(obj -> (DeviceProjection) obj)
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
