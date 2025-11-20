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

## Documentation

- [TESTING-GUIDE.md](TESTING-GUIDE.md) - Testing strategy
- [FUTURE-IMPROVEMENTS.md](FUTURE-IMPROVEMENTS.md) - Potential enhancements
