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
