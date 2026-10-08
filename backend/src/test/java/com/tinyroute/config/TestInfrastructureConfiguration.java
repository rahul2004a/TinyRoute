package com.tinyroute.config;

import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Shared disposable infrastructure; Ryuk cleans up once the test JVM exits. */
@TestConfiguration(proxyBeanMethods = false)
public class TestInfrastructureConfiguration {

    @Bean
    public DynamicPropertyRegistrar testDatastoreProperties() {
        return registry -> {
            registry.add("spring.datasource.url", Containers.POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", Containers.POSTGRES::getUsername);
            registry.add("spring.datasource.password", Containers.POSTGRES::getPassword);
            // Cached Spring test contexts share one small disposable database.
            registry.add("spring.datasource.hikari.maximum-pool-size", () -> 4);
            registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
            registry.add("spring.data.redis.host", Containers.REDIS::getHost);
            registry.add("spring.data.redis.port", () -> Containers.REDIS.getMappedPort(6379));
        };
    }

    private static final class Containers {
        private static final PostgreSQLContainer POSTGRES =
                new PostgreSQLContainer(DockerImageName.parse("postgres:17.2-alpine"));
        private static final GenericContainer<?> REDIS =
                new GenericContainer<>(DockerImageName.parse("redis:7.4.2-alpine"))
                        .withExposedPorts(6379)
                        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

        static {
            Startables.deepStart(Stream.of(POSTGRES, REDIS)).join();
        }
    }
}
