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

## Summary

- **Real implementations over mocks** (test behavior)
- **Relational DBs need real databases** (TestContainers)
- **Non-relational can use in-memory fakes**
- **Unit tests per entry point** (not per class)
- **Mock boundaries, not repositories**
- **Bean whitelisting for focused tests**
- **Clear structure** (Given-When-Then, @DisplayName, List.of() + forEach)

---

## References

- [CQRS Lab](https://github.com/tpierrain/CQRS/blob/master/LabInstructions.md)
