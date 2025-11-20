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
