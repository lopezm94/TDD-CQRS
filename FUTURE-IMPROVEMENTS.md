# Future Improvements

## Null Safety
- Migrate to Spring Boot 4 (when Java module compatibility resolved)
- Add JSpecify + NullAway for compile-time null safety

## Testing
- Add mutation testing framework (PIT) to verify test quality

## Persistence
- Replace JPA with jOOQ for type-safe SQL and better query control
- Benefits: compile-time query validation, no hidden queries, explicit SQL
- Consider ClickHouse for telemetry storage (append-only workload)
  - Columnar storage optimized for time-series data
  - Superior compression (10x-100x vs PostgreSQL for telemetry)
  - Fast aggregation queries (avg/min/max temperature over time ranges)
  - Horizontal scalability for high ingestion rates
  - Built-in time-series functions and materialized views

## Error Handling
- Global error handling middleware with consistent response format
- Dead letter queue (DLQ) processing for failed events

## Performance
- Virtual threads (Java 21) for request handling
- Connection pool tuning

## Observability
- Structured logging (JSON format)
- OpenTelemetry integration for distributed tracing
- Metrics export (Prometheus)
