# IFCO Telemetry Service

A CQRS-based telemetry recording and querying service built with Spring Boot.

## Time Spent

**Total development time: 12 hours**

## Requirements

- **Docker** (with Docker Compose)
- **For development/testing:**
  - Java 21
  - Maven (wrapper included)

## Running Tests

```bash
./mvnw clean test
# Clean up testcontainers
./scripts/kill-testcontainers.sh
```

**Test coverage:** 32 tests (100% passing)

## Running the Application

### Start the Application

```bash
./scripts/bootstrap-app.sh
```

This will build the JAR, start all services (PostgreSQL, Kafka, Redis, App), and verify health checks.

The application will be available at `http://localhost:8080`

### Check Health

```bash
curl http://localhost:8080/actuator/health | jq
```

### Record Telemetry Data

```bash
curl -H 'Content-Type: application/json' -d '{ "deviceId":1, "measurement":10, "date": "2025-01-31T13:00:00Z"}' -X POST http://localhost:8080/telemetry
curl -H 'Content-Type: application/json' -d '{ "deviceId":2, "measurement": 8, "date": "2025-01-31T13:00:01Z"}' -X POST http://localhost:8080/telemetry
curl -H 'Content-Type: application/json' -d '{ "deviceId":1, "measurement":12, "date": "2025-01-31T13:00:05Z"}' -X POST http://localhost:8080/telemetry
curl -H 'Content-Type: application/json' -d '{ "deviceId":2, "measurement":19, "date": "2025-01-31T13:00:06Z"}' -X POST http://localhost:8080/telemetry
curl -H 'Content-Type: application/json' -d '{ "deviceId":2, "measurement":10, "date": "2025-01-31T13:00:11Z"}' -X POST http://localhost:8080/telemetry
```

**Expected response:** `202 Accepted`

### Query Latest Temperatures

```bash
curl http://localhost:8080/devices/temperatures | jq
```

### Monitor Devices in Real-Time

```bash
./scripts/monitor-devices.sh
```

Queries the API every second and displays results in a live-updating table. Press Ctrl+C to stop.

## Shutdown

```bash
docker-compose down
```

Data is not persisted between runs.

## Architecture

CQRS with event sourcing:
- **Write side:** PostgreSQL (command handler → event publisher)
- **Event processing:** Kafka (async event delivery)
- **Read side:** Redis (projections for queries)

## Edge Cases

### 1. What happens if you receive a telemetry that is older than the latest status?

**Answer:** The system ignores older telemetry and keeps the latest measurement based on timestamp.

**Implementation:** The `TelemetryRecordedEventHandler` compares the incoming event's timestamp with the stored projection's `lastUpdated` timestamp. If the incoming event is older (`event.date().isBefore(projection.getLastUpdated())`), it's ignored and logged as "Ignored older event".

**Example:**
```bash
# Send newer measurement first
curl -X POST http://localhost:8080/telemetry \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":1, "measurement":20, "date":"2025-01-31T13:00:10Z"}'

# Send older measurement - will be ignored
curl -X POST http://localhost:8080/telemetry \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":1, "measurement":15, "date":"2025-01-31T13:00:05Z"}'

# Query shows only the newer measurement (20)
curl http://localhost:8080/devices/temperatures
```

### 2. What happens if by mistake, you receive the same telemetry twice?

**Answer:** The system uses "last write wins" semantics for the same timestamp.

**Implementation:** When an event has the same timestamp as the stored projection (`event.date().equals(projection.getLastUpdated())`), the system updates the measurement with the new value. This ensures eventual consistency even with duplicate events or timestamp conflicts.

**Example:**
```bash
# Send measurement
curl -X POST http://localhost:8080/telemetry \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":1, "measurement":25, "date":"2025-01-31T13:00:15Z"}'

# Send duplicate (same timestamp) - last write wins
curl -X POST http://localhost:8080/telemetry \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":1, "measurement":25, "date":"2025-01-31T13:00:15Z"}'

# Query shows the measurement (idempotent for same value)
curl http://localhost:8080/devices/temperatures
```

**Key Design Decision:** The system prioritizes timestamp-based ordering over event ordering, making it resilient to out-of-order delivery and network retries common in distributed systems.

## Documentation

- [TESTING-GUIDE.md](TESTING-GUIDE.md) - Testing strategy
- [FUTURE-IMPROVEMENTS.md](FUTURE-IMPROVEMENTS.md) - Potential enhancements
