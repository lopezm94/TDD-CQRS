# Claude Development Guide

## Quick Start for New Sessions

**Read these files in order:**
1. @REQUIREMENTS.md - Code quality standards
2. @REQUIREMENTS-EXAMPLES.md - Practical examples
3. @WIP.md - Current progress and next steps
4. @ADR.md - Architecture decisions with rationale
5. @NON-GOALS.md - What we won't implement (scope boundaries)
6. @FUTURE-IMPROVEMENTS.md - Potential enhancements (not prioritized)

## Core Philosophy

### Outside-In Test-Driven Development

Based on CQRS lab methodology with our improvements.

**Double-loop TDD approach:**

**Outer loop (Acceptance Test):**
1. Write acceptance test for user-facing behavior (RED)
2. Test fails (no implementation yet)
3. Enter inner loop to build components

**Inner loop (Unit Tests):**
1. Write unit test for component (RED)
2. Implement minimal code (GREEN)
3. Refactor while keeping tests green
4. Repeat for each component

**Complete outer loop:**
1. All components done
2. Acceptance test passes (GREEN)
3. Refactor entire flow
4. Commit

**Key principle:** Acceptance tests drive what to build, unit tests drive how to build it.

**Confirmation points:**
- **After writing tests**: Get user confirmation before implementing
- **After implementation**: Get user confirmation before committing

### Behavior Over Implementation

**CRITICAL: Read @ADR.md (ADR-002) for complete testing strategy**

**Test observable outcomes:**
```java
// ❌ Testing implementation
verify(repository).save(any());

// ✅ Testing behavior
assertThat(repository.findAll()).hasSize(1);
```

**Key principles from ADR-002:**
- Use real implementations, not mocks
- Unit tests per entry point (not per class)
- Entry points: HTTP endpoints, event consumers, command/query handlers
- Don't unit test internal classes
- Acceptance tests are primary TDD driver

**Test code style:**
- **Always use `@DisplayName` annotations** to describe test cases in plain English
- Example: `@DisplayName("Should create projection for first telemetry event")`
- When testing multiple similar operations, define data upfront and iterate
- Example: `List.of(events).forEach(handler::handle)` instead of multiple handler calls
- Reduces verbosity while maintaining clarity
- Applies to commands, events, and query setup
- Use Given-When-Then structure with clear comments

## Development Workflow

### Incremental Dependencies

Add when needed, not upfront:
- Iteration 0: Web + Lombok
- Iteration 1: JPA + PostgreSQL + Flyway + TestContainers
- Later: Kafka, Redis as needed

### Abstraction for TDD Progression

**Create interfaces when:**
- Enables simple → complex progression
- Tests unchanged when swapping implementations
- Example: InMemory → Redis

**Don't create when:**
- Only one implementation
- Framework provides abstraction
- Over-engineering (YAGNI)

### Iteration Pattern

Each iteration:
1. Plan features
2. Write failing acceptance test (Red) → **Get user confirmation**
3. Write failing unit tests (Red) → **Get user confirmation**
4. Implement (Green)
5. Refactor
6. All tests pass (`./mvnw clean test`)
7. **Verify docker-compose works** (`./scripts/test-docker-compose.sh`)
   - Note: This script automatically runs `./mvnw clean package -DskipTests` first
   - No need to build jar separately before running this script
8. **Update WIP.md and TDD-PLAN.md** to reflect completed work
9. **Get user confirmation before commit**
10. Commit with conventional format
11. Push to main

## Commit Strategy

**Always get user confirmation before committing**

Show user what will be committed:
```bash
git status
```

Conventional commit format:
- `feat:` - New feature
- `test:` - Tests
- `refactor:` - Code improvements
- `chore:` - Dependencies, config
- `fix:` - Bug fix
- `docs:` - Documentation

## Key Standards

From `REQUIREMENTS.md`:
- Records for immutable data (commands, events, DTOs)
- Classes + Lombok for mutable (entities, projections)
- Optional for return types, never parameters/fields
- Given-When-Then test structure
- Awaitility for async testing
- TestContainers for relational DBs
- In-memory for non-relational

## Infrastructure Testing

**Before committing, verify docker-compose works:**
```bash
./scripts/test-docker-compose.sh
```

This script:
- Starts all services
- Waits for health checks
- Tests health endpoint
- Cleans up

Health endpoint should be implemented early to enable infrastructure testing.

## Files Never Commit

In `.gitignore`:
- `WIP.md` - Session tracker (personal notes)

## Example Workflow

```bash
# Check current state
cat WIP.md

# Write failing test (RED)
./mvnw test  # Fails

# Implement (GREEN)
./mvnw test  # Passes

# Refactor
./mvnw test  # Still passes

# Update WIP.md

# Get user confirmation
git status

# Commit after approval
git add -A
git commit -m "feat: implement X"
git push origin main
```

## Maintaining TDD-PLAN.md

**When updating completed iterations:**
- Update the plan to reflect what was actually done
- Don't add "What we actually did" or "Created:" sections
- Rewrite the iteration as if we planned it that way
- Don't track drift between plan and reality
- Keep the plan as the source of truth for what was done
- **Preserve the original structure as much as possible**
- Update existing steps/sections rather than adding new ones
- Keep the same wording and format, just update code examples and descriptions

## Remember

- Read REQUIREMENTS.md for code standards
- Check WIP.md for current state
- Follow TDD strictly
- Test behaviors, not implementation
- Real implementations over mocks
- User confirmation before commits
- Incremental complexity (YAGNI)
