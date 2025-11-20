# Future Improvements

## Null Safety
- Migrate to Spring Boot 4 (when Java module compatibility resolved)
- Add JSpecify + NullAway for compile-time null safety

## Testing
- Add mutation testing framework (PIT) to verify test quality
- Add acceptance tests that exercise the full HTTP → Handler → Event → Projection flow
  - Current acceptance tests call handlers directly (unit-style)
  - Would benefit from controller-level tests using MockMvc or TestRestTemplate
  - Validates request validation, HTTP status codes, error responses

## Validation
- Use Bean Validation annotations (`@NotNull`, `@Valid`) instead of explicit null checks
  - Apply `@Validated` to command handlers to enable method-level validation
  - Benefits: Declarative, less boilerplate, consistent with controller validation
  - Example:
    ```java
    public record RecordTelemetryCommand(
        @NotNull Long deviceId,
        @NotNull Double temperature,
        @NotNull Instant date
    ) {}
    
    @Component
    @Validated
    class RecordTelemetryCommandHandler {
        public void handle(@Valid RecordTelemetryCommand command) {
            // Validation happens automatically
        }
    }
    ```

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

## Resiliency & Scalability
- **Event batching**: Batch multiple events before publishing to Kafka
  - Reduces network overhead and improves throughput
  - Trade-off: Slightly increased latency vs better performance
  - Configure batch size and linger time based on load
  - Should prevent consumer lag issues, making backpressure unnecessary
- **Circuit breaker pattern** for downstream dependencies (Kafka, Redis, PostgreSQL)
  - Fail fast when services are unavailable
  - Automatic recovery detection
- **Note on backpressure**: Only needed at very high scale (millions of devices)
  - With proper batching and Kafka's built-in buffering, consumer lag should not occur
  - Monitor consumer lag metrics before adding complexity

## Performance
- Virtual threads (Java 21) for request handling
- Connection pool tuning

## Observability
- Structured logging (JSON format)
- OpenTelemetry integration for distributed tracing
- Metrics export (Prometheus)
