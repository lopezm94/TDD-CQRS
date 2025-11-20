# Testing Guide

> Complete testing strategy for the telemetry service

## Core Principle

**Behavior Over Implementation**: Tests must ALWAYS TEST BEHAVIORS; NOT IMPLEMENTATIONS.

```java
// ❌ Testing implementation
verify(repository).save(any());

// ✅ Testing behavior
assertThat(repository.findAll()).hasSize(1);
```

---

## Testing Strategy

### Decision

1. Use **real implementations** (not mocks) to test behavior
2. **Relational DB repositories**: Use real database (TestContainers PostgreSQL)
3. **Non-relational repositories**: In-memory fakes acceptable (e.g., Redis → ConcurrentHashMap)

### Testing Approach

**Acceptance Tests:**
- Full flow with all components
- Primary TDD driver (Outside-In)
- Test complete behaviors from entry point to outcome
- **Do NOT use `@Transactional`** - see rationale below

**Why Acceptance Tests Should NOT Use `@Transactional`:**

1. **Test real behavior** - Acceptance tests should verify actual transaction boundaries, not artificial test-only transactions
2. **No test-specific code** - Production code shouldn't be modified (e.g., adding `flush()`) just to accommodate test infrastructure
3. **Explicit cleanup is clearer** - Use `@BeforeEach` to explicitly clean data, making test isolation obvious and predictable
4. **Avoid Hibernate session issues** - `@Transactional` + exceptions = corrupted Hibernate session, requiring workarounds like `@DirtiesContext`

**Example:**
```java
@SpringBootTest
// NO @Transactional - test real transaction behavior
class TelemetryErrorScenariosAcceptanceTest {
    
    @BeforeEach
    void clearData() {
        // Explicit cleanup for test isolation
        telemetryRepository.deleteAll();
        projectionRepository.deleteAll();
    }
    
    @Test
    void should_reject_invalid_input() {
        assertThatThrownBy(() -> handler.handle(invalidCommand))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Can safely verify - no dirty Hibernate session
        assertThat(repository.findAll()).isEmpty();
    }
}
```

**Unit Tests:**
- Written **per entry point** (not per class)
- Entry points: HTTP endpoints, event consumers, command/query handlers
- Two types:

  **1. Entry Point Tests (Controllers, Kafka Listeners):**
  - Use `@WebMvcTest` or similar
  - **Mock the handler** to verify correct parameters passed
  - Test: Does entry point extract/transform data correctly?

  **2. Handler Tests (Command/Query Handlers):**
  - Use `@SpringBootTest` with real beans
  - Test observable behavior, not internals
  - Test: Does handler produce correct outcomes?

**When mocking is acceptable:**
- Entry point tests mocking handlers
- Mocking at the boundary to isolate entry point logic from business logic
- Never mock repositories or domain logic in handler tests

**Unit test boundaries:**
- Handler tests should NOT cross boundaries (e.g., command handler → event handler → projection)
- Each handler tests only its immediate behavior
- Cross-boundary flows covered by acceptance tests

**What NOT to unit test:**
- Internal classes (not entry points)
- Implementation details already covered by acceptance tests
- Behavior of downstream components
- **Thin adapters** (Hexagonal Architecture plumbing - see below)

**Thin Adapters (Hexagonal Architecture):**
- Adapters with no transformation logic don't need separate unit tests
- Examples: `@KafkaListener` that just calls handler, `@RestController` that just delegates
- **If adapter has transformation**: Add entry point test mocking the handler
- **If adapter is pure delegation**: Skip entry point test, covered by acceptance tests
- Our `TelemetryRecordedEventHandler` with `@KafkaListener`: No transformation → No separate entry point test needed
- Acceptance tests verify the full Kafka → Handler → Projection flow

---

## Test Organization

**Package structure by architectural role:**
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

**Why organize by role:**
- Makes testing strategy visible in structure
- All entry points tested the same way (mock handlers)
- All handlers tested the same way (real beans)
- Consistent patterns across technologies

**Naming Convention:**
- Unit tests: `*UnitTest.java`
- Acceptance tests: `*AcceptanceTest.java`
- Entry point tests: `*Test.java`

---

## Handler Unit Test Configuration

**UnitTestConfiguration** provides mocked boundaries and JPA config:

```java
@TestConfiguration
@EnableJpaRepositories(basePackages = "com.ifco.telemetry.repository")
@EntityScan(basePackages = "com.ifco.telemetry.domain")
public class UnitTestConfiguration {
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return Mockito.mock(EventPublisher.class);  // Mock boundary between handlers
    }
}
```

**Handler unit test structure:**

```java
@SpringBootTest(classes = { RecordTelemetryCommandHandler.class })
@EnableAutoConfiguration  // JPA, DataSource, Transaction infrastructure
@Import(UnitTestConfiguration.class)  // Mocked boundaries + JPA config
class RecordTelemetryCommandHandlerUnitTest extends TestContainersBase {

    @Autowired private RecordTelemetryCommandHandler handler;
    @Autowired private TelemetryRepository repository;  // Real
    @Autowired private EventPublisher eventPublisher;  // Mocked

    @BeforeEach
    void clearData() {
        repository.deleteAll();
        Mockito.reset(eventPublisher);  // Reset mock for test isolation
    }

    @Test
    void should_publish_event_with_correct_values() {
        // Verify values passed to mocked boundary
        ArgumentCaptor<TelemetryRecordedEvent> eventCaptor = ...;
        verify(eventPublisher).publish(eventCaptor.capture());

        assertThat(eventCaptor.getValue().deviceId()).isEqualTo(1L);
        // Command handler doesn't care HOW EventPublisher works internally
    }
}
```

**Key principles:**
- **Bean whitelisting**: Only load handler + required dependencies
- **UnitTestConfiguration**: Centralizes mocked boundaries (@Primary) and JPA config
- **JPA config in test config**: `@EnableJpaRepositories`/`@EntityScan` needed when bypassing auto-configuration
- **Mock reset in @BeforeEach**: Only reset mocks where they exist
- **Verify boundary interactions**: Use `ArgumentCaptor`
- **Test isolation**: Spring reuses context, mocks must be reset
- **Real repositories**: Use TestContainers for behavior testing

**Why mock boundaries:**

**CRITICAL: Mock the boundary from YOUR component's perspective**

Boundaries are **output interfaces between architectural units**, not implementation details.

**From your component's perspective:**
- Command Handler → `EventPublisher` (boundary between command/event handlers)
  - NOT `KafkaTemplate` (internal implementation of EventPublisher)
- Service → `RestClient` (boundary to external API)
  - NOT `HttpClient` (internal implementation of RestClient)

**Why this matters:**
- Tests remain stable when implementation changes
- If EventPublisher switches from Kafka to RabbitMQ, tests don't break
- Tests verify component's contract, not how dependencies work internally
- Boundary behavior tested separately in its own tests
- Acceptance tests verify full cross-boundary flow

**Naming convention:**
- Mock variables should have `Mock` suffix: `eventPublisherMock`, `restClientMock`
- Makes it clear what's real vs mocked in test code

---

## Repository Testing

| Type | Implementation | Reason |
|------|----------------|---------|
| Relational (JPA) | TestContainers PostgreSQL | Needs `@Transactional`, query methods, constraints |
| Non-relational (Redis) | In-memory fake | Simple key-value, no DB features needed |

**Why not in-memory for relational DBs:**
- `@Transactional` won't function
- Spring Data query methods need real database
- Missing joins, constraints, indexing

**Why TestContainers (not H2):**
- Production-like PostgreSQL behavior
- `@Transactional` rollback ensures isolation
- Container reuse = fast startup
- H2 has SQL dialect differences

**Future optimization:** H2 for unit tests only if parallel execution becomes bottleneck (YAGNI)

---

## Test Code Style

**Always use @DisplayName:**
```java
@DisplayName("Should create projection for first telemetry event")
```

**Given-When-Then structure:**
```java
// Given
var command = new RecordTelemetryCommand(1L, 10.0, now());
// When
handler.handle(command);
// Then
assertThat(repository.findAll()).hasSize(1);
```

**List.of() + forEach pattern:**
```java
// Given
List<RecordTelemetryCommand> commands = List.of(
    new RecordTelemetryCommand(1L, 10.0, timestamp1),
    new RecordTelemetryCommand(2L, 15.0, timestamp2)
);
// When
commands.forEach(handler::handle);
// Then
assertThat(repository.findAll()).hasSize(2);
```

**Async testing with Awaitility:**
```java
await().atMost(Duration.ofSeconds(5))
    .untilAsserted(() -> assertThat(repo.findById(1L)).isPresent());
```

**Cleanup in @BeforeEach (not @AfterEach):**
```java
@BeforeEach
void clearData() {
    repository.deleteAll();
    Mockito.reset(kafkaTemplate);  // Only if using mocks
}
```

---

## Hexagonal Architecture (Ports and Adapters)

**Core concept:** Wrap external communications behind interfaces (ports), with implementations as adapters.

**When to create interfaces:**
- Communication with external systems (databases, message brokers, APIs)
- Enables swapping implementations without breaking tests
- Supports TDD progression: simple → complex

**Examples in this codebase:**

**1. ProjectionRepository (Port):**
```java
public interface ProjectionRepository {
    Optional<DeviceProjection> findById(Long deviceId);
    void save(DeviceProjection projection);
    Iterable<DeviceProjection> findAll();
    void deleteAll();
}
```

**Adapters (swapped without changing tests):**
- Iteration 1: `InMemoryProjectionRepository` (ConcurrentHashMap)
- Iteration 4: `RedisProjectionRepository` (Redis with JSON serialization)

**2. EventPublisher (Port):**
```java
public interface EventPublisher {
    void publish(Object event);
}
```

**Adapters:**
- Iteration 1: Synchronous in-memory delivery
- Iteration 3: Kafka-based async messaging via `KafkaTemplate`

**Benefits:**
- **Test stability**: Tests depend on interface, not implementation
- **TDD progression**: Start simple (in-memory), evolve to production (Redis/Kafka)
- **Implementation independence**: Business logic doesn't know about Redis/Kafka specifics
- **Easy mocking**: Mock the port in tests, not the infrastructure

**When NOT to create interfaces:**
- Single implementation with no planned alternatives
- Framework already provides abstraction (e.g., Spring Data repositories)
- Over-engineering with no clear benefit (YAGNI)

**Architecture layers:**
```
Application Core (Handlers, Domain)
         ↓ uses
    Ports (Interfaces)
         ↓ implemented by
  Adapters (Redis, Kafka, PostgreSQL)
```

**Key principle:** Domain logic depends on ports (abstractions), never directly on adapters (concrete infrastructure).

---

## Summary

- **Real implementations over mocks** (test behavior)
- **Relational DBs need real databases** (TestContainers)
- **Non-relational can use in-memory fakes**
- **Unit tests per entry point** (not per class)
- **Mock boundaries, not repositories**
- **Bean whitelisting for focused tests**
- **Clear structure** (Given-When-Then, @DisplayName, List.of() + forEach)
- **Hexagonal Architecture** for external communications (wrap in interfaces)

---

## References

- [CQRS Lab](https://github.com/tpierrain/CQRS/blob/master/LabInstructions.md)
- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
