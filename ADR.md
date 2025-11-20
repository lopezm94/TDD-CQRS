# Architecture Decision Records

## ADR-001: Skip Null-Safety Tooling (JSpecify/NullAway/Checker Framework)

**Date:** 2024-11-20  
**Status:** Accepted

### Decision

Skip all null-safety annotation and enforcement tooling (JSpecify, NullAway, Checker Framework, SpotBugs) for this project.

### Context

Attempted to set up compile-time null-safety checking with JSpecify + NullAway, then tried Checker Framework as an alternative. All tools encountered Java module system compatibility issues with Spring Boot 3.x.

### Why We Skipped Null-Safety Tooling

1. **Java module system conflicts** - All checkers (NullAway, Checker Framework) require internal compiler API access via `--add-exports`, causing `IllegalAccessError` even with proper flags
2. **Spring Boot 3.x incompatibility** - Null-safety tooling is designed for Spring Boot 4.0+, not fully compatible with 3.x
3. **Build complexity** - Requires forked compiler process, extensive JVM flags, and complex annotation processor configuration
4. **Time constraint** - Interview challenge scope; debugging tooling infrastructure detracts from implementing business features
5. **Not essential for core functionality** - Null-safety is nice-to-have, TDD with comprehensive tests provides sufficient coverage

### Future Consideration

If null-safety becomes critical, consider upgrading to Spring Boot 4.0 when stable, which has first-class JSpecify support.

### References

- [Spring Boot 4.0 Null-Safety with JSpecify](https://spring.io/blog/2025/03/10/null-safety-in-spring-apps-with-jspecify-and-null-away)
- [JSpecify 1.0 Specification](https://jspecify.dev/)

---

## ADR-002: Testing Strategy - Behavior-Focused with Real Implementations

**Date:** 2024-11-20  
**Status:** Accepted

### Decision

1. Use **real implementations** (not mocks) to test behavior
2. **Relational DB repositories**: Use real database (TestContainers PostgreSQL)
3. **Non-relational repositories**: In-memory fakes are acceptable (e.g., Redis → ConcurrentHashMap)

### Context

Following CQRS lab: "tests (including unit ones) must ALWAYS TEST BEHAVIOURS; NOT IMPLEMENTATIONS."

### Testing Approach

**Acceptance Tests:**
- Full flow with all components
- Primary TDD driver (Outside-In)
- Test complete behaviors from entry point to outcome

**Unit Tests:**
- Written **per entry point** (not per class)
- Entry points: HTTP endpoints, event consumers, command/query handlers
- Two types of unit tests:

  **1. Entry Point Tests (Controllers, Kafka Listeners):**
  - Use `@WebMvcTest` or similar
  - **Mock the handler** to verify correct parameters passed
  - Test: Does entry point extract/transform data correctly?
  - Example: Controller receives HTTP request → creates correct command → calls handler
  
  **2. Handler Tests (Command/Query Handlers):**
  - Use `@SpringBootTest` with real beans
  - Test observable behavior, not internals
  - Test: Does handler produce correct outcomes?
  - Example: Handler receives command → correct data in repository

**When mocking is acceptable:**
- Entry point tests mocking handlers (verify parameter transformation)
- Mocking at the boundary to isolate entry point logic from business logic
- Never mock repositories or domain logic in handler tests

**Unit test boundaries:**
- Handler tests should NOT cross boundaries (e.g., command handler → event publisher → event handler → projection)
- Each handler tests only its immediate behavior
- Cross-boundary flows covered by acceptance tests
- Example: Command handler test verifies telemetry saved, NOT that projection updated (that's event handler's test)

**What NOT to unit test:**
- Internal classes (not entry points)
- Implementation details already covered by acceptance tests
- Behavior of downstream components (test those components directly)

### Test Organization

**Test package structure follows architectural roles, not implementation:**
```
src/test/java/com/ifco/telemetry/
├── acceptance/           # Full flow tests
├── unit/
│   ├── entrypoint/      # Entry point tests (HTTP, Kafka, etc.)
│   │   ├── controller/  # HTTP entry points (@WebMvcTest, mock handlers)
│   │   └── listener/    # Kafka entry points (mock handlers)
│   ├── command/         # Command handler tests (@SpringBootTest, real beans)
│   ├── query/           # Query handler tests (@SpringBootTest, real beans)
│   └── event/           # Event handler tests (@SpringBootTest, real beans)
```

**Why organize by role instead of type:**
- Makes testing strategy visible in structure
- All entry points (HTTP, Kafka, gRPC) tested the same way (mock handlers)
- All handlers (command, query, event) tested the same way (real beans)
- Easy to find tests: "What's the entry point?" → `unit/entrypoint/`
- Consistent patterns across different technologies (REST controllers, Kafka listeners behave the same architecturally)

### Unit Test Configuration and Naming

**Naming Convention:**
- Unit tests: `*UnitTest.java` (e.g., `RecordTelemetryCommandHandlerUnitTest.java`)
- Acceptance tests: `*AcceptanceTest.java`
- Entry point tests: `*Test.java` (e.g., `TelemetryControllerTest.java`)

This makes it easy to distinguish test types and run them separately.

**Handler Unit Test Configuration (Bean Whitelisting):**
```java
// Base class centralizes TestContainers + UnitTestConfiguration + mock reset
@Import(UnitTestConfiguration.class)
public abstract class HandlerUnitTestBase extends TestContainersBase {
    @Autowired(required = false)
    private EventPublisher eventPublisher;
    
    @BeforeEach
    void resetMocks() {
        if (eventPublisher != null) {
            Mockito.reset(eventPublisher);  // Centralized test isolation
        }
    }
}

// Handler unit tests extend the base
@SpringBootTest(classes = { RecordTelemetryCommandHandler.class })
@EnableAutoConfiguration  // JPA, DataSource, Transaction infrastructure
@EnableJpaRepositories(basePackages = "com.ifco.telemetry.repository")
@EntityScan(basePackages = "com.ifco.telemetry.domain")
class RecordTelemetryCommandHandlerUnitTest extends HandlerUnitTestBase {
    
    @Autowired private TelemetryRepository repository;  // Real
    @Autowired private EventPublisher eventPublisher;   // Mocked via HandlerUnitTestBase
    
    @BeforeEach
    void clearData() {
        repository.deleteAll();
        // Mock reset handled by HandlerUnitTestBase
    }
    
    @Test
    void should_publish_event_with_correct_values() {
        // Verify values passed to mocked boundary
        ArgumentCaptor<TelemetryRecordedEvent> captor = ...;
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().deviceId()).isEqualTo(1L);
    }
}
```

**Key principles:**
- **Bean whitelisting**: Only load handler + required dependencies
- **Mock boundaries**: Use `UnitTestConfiguration` to provide mocked boundary beans (`@Primary`)
- **Centralized mock reset**: `HandlerUnitTestBase` resets mocks automatically in `@BeforeEach`
- **Verify boundary interactions**: Use `ArgumentCaptor` to verify values passed to mocks
- **Test isolation**: Spring reuses context across tests, mocks must be reset between tests
- **Real repositories**: Use TestContainers for behavior testing (not mocks)

**Why mock boundaries:**
- Boundaries are **output interfaces** to external systems (events, APIs, messaging)
- Mocking prevents crossing into different units (event handlers, external services)
- We verify the handler calls the boundary with correct values
- Examples:
  - `EventPublisher` → prevents entering event handler territory
  - `RestClient` / `WebClient` → prevents actual HTTP calls to external APIs
  - `KafkaTemplate` → prevents actual message publishing
- Boundary behavior is tested separately (event handlers in their own unit tests, external APIs in contract/integration tests)
- Acceptance tests verify the full cross-boundary flow

### Repository Testing Strategy

| Repository Type | Test Implementation | Reason |
|----------------|---------------------|---------|
| Relational (JPA) | TestContainers PostgreSQL | Needs `@Transactional`, query methods, constraints |
| Non-relational (Redis) | In-memory fake (ConcurrentHashMap) | Simple key-value, no DB features needed |

**Example:**
- `TelemetryRepository` (JPA): TestContainers PostgreSQL - requires real DB for `@Transactional`
- `ProjectionRepository` (Redis): `InMemoryProjectionRepository` - acceptable fake, swaps to Redis later

### Why Not In-Memory for Relational DBs?

In-memory implementations of relational repositories **don't work** because:
- `@Transactional` annotations won't function
- Spring Data query methods need real database
- Missing joins, constraints, indexing behavior
- Can't test actual SQL execution

### Why TestContainers (Not H2)?

**Shared TestContainers for unit and acceptance tests:**
- Production-like PostgreSQL behavior
- `@Transactional` rollback ensures test isolation
- Container reuse (`.withReuse(true)`) = fast startup
- Simple: one configuration

**Why not H2:**
- SQL dialect differences (PostgreSQL vs H2)
- Different constraint/type/index behavior
- Bugs appear only in production

### Future: Parallel Test Optimization

If parallel execution becomes bottleneck:

**Option: H2 for unit tests**
```java
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
```
**Trade-off:** Faster parallel execution, but PostgreSQL/H2 differences risk bugs

**Current:** Shared TestContainers until performance is actual problem (YAGNI)

### Summary

- **Real implementations over mocks** (test behavior)
- **Relational DBs need real databases** (TestContainers)
- **Non-relational can use in-memory fakes** (simple key-value)

### References

- [CQRS Lab](https://github.com/tpierrain/CQRS/blob/master/LabInstructions.md)
