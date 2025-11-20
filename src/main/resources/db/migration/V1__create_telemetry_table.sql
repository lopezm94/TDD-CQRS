CREATE TABLE telemetry (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Single composite index for queries - no unique constraint
-- This index supports: latest per device, time range queries, etc.
CREATE INDEX idx_telemetry_device_timestamp ON telemetry(device_id, timestamp DESC);

-- Optional: Index for time-based queries across all devices
CREATE INDEX idx_telemetry_timestamp ON telemetry(timestamp DESC);

-- Design Decision: Allow duplicates for better write performance
-- Deduplication happens at query/projection level if needed
