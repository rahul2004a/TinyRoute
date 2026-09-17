package com.tinyroute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest {

    @Test
    void developmentProfileUsesLocalComposeDatastores() {
        Properties properties = loadProperties("application-dev.yml");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:tinyroute}")
                .containsEntry("spring.datasource.username", "${POSTGRES_USER:tinyroute}")
                .containsEntry("spring.datasource.password", "${POSTGRES_PASSWORD:local-dev-password}")
                .containsEntry("spring.data.redis.host", "localhost")
                .containsEntry("spring.data.redis.port", "${REDIS_PORT:6379}");
    }

    @Test
    void productionProfileRequiresEnvironmentProvidedEndpointsAndSecrets() {
        Properties properties = loadProperties("application-prod.yml");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "${DATABASE_URL}")
                .containsEntry("spring.datasource.username", "${DATABASE_USERNAME}")
                .containsEntry("spring.datasource.password", "${DATABASE_PASSWORD}")
                .containsEntry("spring.data.redis.host", "${REDIS_HOST}")
                .containsEntry("spring.data.redis.port", "${REDIS_PORT}");
    }

    private Properties loadProperties(String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);
        assertThat(resource.exists()).isTrue();

        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        return factory.getObject();
    }
}
