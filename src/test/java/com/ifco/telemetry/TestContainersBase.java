package com.ifco.telemetry;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using TestContainers.
 * Uses singleton pattern to share containers across all tests for better performance.
 */
@Testcontainers
public abstract class TestContainersBase {

    // Singleton PostgreSQL container shared across all tests
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        "postgres:17-alpine"
    )
        .withDatabaseName("telemetry_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    // Singleton Kafka container shared across all tests
    static final KafkaContainer KAFKA = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    ).withReuse(true);

    // Singleton Redis container shared across all tests
    static final RedisContainer REDIS = new RedisContainer(
        DockerImageName.parse("redis:7-alpine")
    ).withReuse(true);

    static {
        POSTGRES.start();
        KAFKA.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Kafka configuration
        registry.add(
            "spring.kafka.bootstrap-servers",
            KAFKA::getBootstrapServers
        );
        registry.add(
            "spring.kafka.consumer.bootstrap-servers",
            KAFKA::getBootstrapServers
        );
        registry.add(
            "spring.kafka.producer.bootstrap-servers",
            KAFKA::getBootstrapServers
        );

        // Redis configuration
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
