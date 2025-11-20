# Code Examples

## Optional Usage

```java
// ✅ GOOD
Optional<DeviceProjection> findById(Long id);

// ❌ BAD: Optional parameter
void process(Optional<Long> id);

// ✅ GOOD: Validate instead
void process(Long id) {
    if (id == null) throw new IllegalArgumentException();
}

// ❌ BAD: Optional collection
Optional<List<Device>> getDevices();

// ✅ GOOD
List<Device> getDevices() { return List.of(); }
```

## Domain Models

```java
// Records for immutable data
public record RecordTelemetryCommand(Long deviceId, Double temp, Instant date) {}

// Classes for mutable data
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Telemetry {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private Long deviceId;
}
```

## Testing

```java
// Given-When-Then
@Test
void should_save_telemetry() {
    // Given
    var command = new RecordTelemetryCommand(1L, 10.0, now());
    // When
    handler.handle(command);
    // Then
    verify(repository).save(any());
}

// Async testing
await().atMost(Duration.ofSeconds(5))
    .untilAsserted(() -> assertThat(repo.findById(1L)).isPresent());
```

## Abstraction

```java
// Interface for swappable implementations
public interface ProjectionRepository {
    Optional<DeviceProjection> findById(Long id);
    void save(DeviceProjection projection);
}

// Iteration 1: In-memory
@Repository
public class InMemoryProjectionRepository implements ProjectionRepository {
    private final ConcurrentHashMap<Long, DeviceProjection> store = new ConcurrentHashMap<>();
}

// Iteration 4: Redis (tests unchanged)
@Repository
@Primary
public class RedisProjectionRepository implements ProjectionRepository { }
```
