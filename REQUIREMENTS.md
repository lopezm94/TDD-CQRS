# Code Requirements

## Package Structure
```
com.ifco.telemetry/
├── command/      # Commands (immutable records)
├── domain/       # JPA entities
├── event/        # Events (immutable records)
├── projection/   # Read model projections
├── query/        # Query DTOs
├── repository/   # Repository interfaces/implementations
└── controller/   # REST controllers
```

## Null Safety and Optional
**Use Optional for:**
- Repository find operations
- Method return types where absence is valid

**Never use Optional for:**
- Method parameters (use validation)
- Class fields (breaks serialization)
- Collections (return empty collection)

## Domain Models
- **Records**: Commands, Events, DTOs (immutable)
- **Classes + Lombok**: Entities, Projections (mutable)
  - Required: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

## Testing
- **Behavior over implementation**: Test observable outcomes, not internals
- **Real implementations over mocks**: Use actual beans (see TESTING-GUIDE.md)
- **Unit tests**: Supporting role, `@SpringBootTest` with real beans
- **Acceptance tests**: Primary TDD driver, full flow testing
- Structure: Given-When-Then pattern
- Async: Use Awaitility, never `Thread.sleep()`
- Cleanup: `@BeforeEach`, not `@AfterEach`
- TestContainers: Singleton with `.withReuse(true)`

### Repository Testing
- **Relational DBs** (JPA): 
  - Default: TestContainers PostgreSQL (production-like, shared across unit/acceptance)
  - Alternative for unit tests: H2 in-memory (if simple queries, enables parallel tests)
  - Never: Custom in-memory implementations (break `@Transactional`)
- **Non-relational** (Redis): In-memory fakes acceptable (simple key-value)

**Current approach:** TestContainers for all (YAGNI). Consider H2 for unit tests only if parallel execution becomes bottleneck.

## Abstraction
**Create interfaces when:**
- TDD progression benefit (simple → complex)
- Multiple implementations planned
- Tests need infrastructure independence

**Don't create interfaces when:**
- Only one implementation
- Framework already provides abstraction
- No clear benefit (YAGNI)

## Comments
**Do:**
- Explain class responsibility and why certain design choices
- Document Lombok annotations (what they generate and why needed)
- Clarify complex logic
- Document public APIs

**Don't:**
- Reference iteration numbers or temporary states
- Foreshadow future changes (use TODO if needed)
- State the obvious
- Comment what code already says

---

See `REQUIREMENTS-EXAMPLES.md` for code examples.
