package com.tinyroute.security;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthObservabilityFilterTest {

    @Test
    void boundsLogFieldsWhenRequestMetadataContainsLineBreaksOrSensitivePaths() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            Logger logger = (Logger) LoggerFactory.getLogger(AuthObservabilityFilter.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                var request =
                        new MockHttpServletRequest("GET\r\nforged", "/api/auth/secret\nforged");
                new AuthObservabilityFilter(registry)
                        .doFilter(
                                request,
                                new MockHttpServletResponse(),
                                (ignoredRequest, ignoredResponse) -> {});
                String message = appender.list.getFirst().getFormattedMessage();
                var record = new tools.jackson.databind.json.JsonMapper().readTree(message);
                assertThat(record.get("method").asString()).isEqualTo("OTHER");
                assertThat(record.get("path").asString()).isEqualTo("/api/auth/*");
                assertThat(message).doesNotContain("secret", "forged", "\r", "\n");
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        } finally {
            registry.close();
        }
    }

    @Test
    void recordsAuthOutcomesWithoutRequestSecretsInMetricsOrLogs() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            AuthObservabilityFilter filter = new AuthObservabilityFilter(registry);
            Logger logger = (Logger) LoggerFactory.getLogger(AuthObservabilityFilter.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                MockHttpServletRequest request =
                        new MockHttpServletRequest("POST", "/api/auth/login");
                request.setQueryString("token=reset-secret-123");
                request.addHeader("Authorization", "Bearer access-secret-456");
                request.addHeader("Cookie", "refresh=refresh-secret-789");
                request.setContent("{\"password\":\"password-secret-000\"}".getBytes());
                MockHttpServletResponse response = new MockHttpServletResponse();

                filter.doFilter(
                        request,
                        response,
                        (ignoredRequest, ignoredResponse) ->
                                ((HttpServletResponse) ignoredResponse).setStatus(401));

                assertThat(
                                registry.get("tinyroute.auth.requests")
                                        .tags("route", "login", "status", "401")
                                        .counter()
                                        .count())
                        .isEqualTo(1);
                String evidence = registry.getMeters().toString() + appender.list.toString();
                assertThat(evidence).contains("login", "401", "durationMs");
                var logRecord =
                        new tools.jackson.databind.json.JsonMapper()
                                .readTree(appender.list.getFirst().getFormattedMessage());
                assertThat(logRecord.get("method").asString()).isEqualTo("POST");
                assertThat(logRecord.get("path").asString()).isEqualTo("/api/auth/login");
                assertThat(logRecord.get("status").asInt()).isEqualTo(401);
                assertThat(evidence)
                        .doesNotContain(
                                "reset-secret-123",
                                "access-secret-456",
                                "refresh-secret-789",
                                "password-secret-000");
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        } finally {
            registry.close();
        }
    }

    @Test
    void usesABoundedRouteNameForUnknownAuthPaths() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            AuthObservabilityFilter filter = new AuthObservabilityFilter(registry);
            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", "/api/auth/secret-in-path");

            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (ignoredRequest, ignoredResponse) -> {});

            assertThat(
                            registry.get("tinyroute.auth.requests")
                                    .tags("route", "unmatched", "status", "200")
                                    .counter()
                                    .count())
                    .isEqualTo(1);
            assertThat(registry.getMeters().toString()).doesNotContain("secret-in-path");
        } finally {
            registry.close();
        }
    }
}
