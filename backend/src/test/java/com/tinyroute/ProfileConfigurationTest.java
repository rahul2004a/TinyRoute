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
                .containsEntry("spring.data.redis.port", "${REDIS_PORT:6379}")
                .doesNotContainKey("tinyroute.rate-limit.trusted-proxy-cidrs");
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

    @Test
    void mailDeliveryUsesFiniteConnectionReadAndWriteTimeoutsInEveryProfile() {
        for (String profile : java.util.List.of("application-dev.yml", "application-prod.yml")) {
            Properties properties = loadProperties(profile);

            assertThat(properties)
                    .containsEntry("spring.mail.properties[mail.smtp.connectiontimeout]", 5000)
                    .containsEntry("spring.mail.properties[mail.smtp.timeout]", 5000)
                    .containsEntry("spring.mail.properties[mail.smtp.writetimeout]", 5000);
        }
    }

    @Test
    void developmentProfileCanAuthenticateToAnSmtpServerWithRequiredStartTls() {
        Properties properties = loadProperties("application-dev.yml");

        assertThat(properties)
                .containsEntry("spring.mail.username", "${MAIL_USERNAME:}")
                .containsEntry("spring.mail.password", "${MAIL_PASSWORD:}")
                .containsEntry("spring.mail.properties[mail.smtp.auth]", "${MAIL_SMTP_AUTH:false}")
                .containsEntry("spring.mail.properties[mail.smtp.starttls.enable]", "${MAIL_SMTP_STARTTLS:false}")
                .containsEntry("spring.mail.properties[mail.smtp.starttls.required]", "${MAIL_SMTP_STARTTLS:false}");
    }

    @Test
    void developmentProfileServesHttpsWithLocalCertificate() {
        Properties properties = loadProperties("application-dev.yml");

        assertThat(properties)
                .containsEntry("server.port", 8443)
                .containsEntry("server.ssl.certificate", "${DEV_TLS_CERTIFICATE:file:../.local-certs/localhost.pem}")
                .containsEntry("server.ssl.certificate-private-key", "${DEV_TLS_PRIVATE_KEY:file:../.local-certs/localhost-key.pem}");
    }

    private Properties loadProperties(String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);
        assertThat(resource.exists()).isTrue();

        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        return factory.getObject();
    }
}
