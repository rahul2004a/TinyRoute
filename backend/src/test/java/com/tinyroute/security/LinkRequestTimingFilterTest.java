package com.tinyroute.security;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LinkRequestTimingFilterTest {
    @Test
    void includesDownstreamSecurityAndThrottleTimeUsingOnlyFixedSafeFields() throws Exception {
        var registry = new SimpleMeterRegistry();
        var filter = new LinkRequestTimingFilter(registry, new PublicRedirectRequestMatcher());
        var logger = (Logger) LoggerFactory.getLogger(LinkRequestTimingFilter.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("GET", "/private-code");
            request.setQueryString("destination=private-destination");
            request.addHeader("Cookie", "private-cookie");
            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (req, res) -> {
                        long end = System.nanoTime() + 5_000_000;
                        while (System.nanoTime() < end) Thread.onSpinWait();
                        ((jakarta.servlet.http.HttpServletResponse) res).setStatus(429);
                    });
            var timer =
                    registry.get("tinyroute.links.duration")
                            .tags("route", "redirect", "method", "GET", "status", "429")
                            .timer();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS))
                    .isGreaterThanOrEqualTo(5_000_000);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("route=redirect", "method=GET", "status=429", "durationNanos=")
                    .doesNotContain("private-code", "private-destination", "private-cookie");
            assertThat(timer.getId().getTags()).hasSize(3);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            registry.close();
        }
    }

    @Test
    void recordsThrownFailuresAs500WithoutChangingTheResponseOrLeakingAnException() {
        var registry = new SimpleMeterRegistry();
        var filter = new LinkRequestTimingFilter(registry, new PublicRedirectRequestMatcher());
        var response = new MockHttpServletResponse();
        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        new MockHttpServletRequest("POST", "/api/links"),
                                        response,
                                        (req, res) -> {
                                            throw new ServletException("private-owner");
                                        }))
                .isInstanceOf(ServletException.class);
        assertThat(
                        registry.get("tinyroute.links.duration")
                                .tags("route", "create", "method", "POST", "status", "500")
                                .timer()
                                .count())
                .isEqualTo(1);
        assertThat(response.getHeader("Location")).isNull();
    }

    @Test
    void ignoresAuthHealthAndFixtureRoutesAndRecordsHeadWithFixedLabels() throws Exception {
        var registry = new SimpleMeterRegistry();
        var filter = new LinkRequestTimingFilter(registry, new PublicRedirectRequestMatcher());
        for (String path :
                java.util.List.of("/api/auth/me", "/actuator/health", "/__verification/timings"))
            filter.doFilter(
                    new MockHttpServletRequest("GET", path),
                    new MockHttpServletResponse(),
                    (req, res) -> {});
        assertThat(registry.getMeters()).isEmpty();
        filter.doFilter(
                new MockHttpServletRequest("HEAD", "/AbC123"),
                new MockHttpServletResponse(),
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(302));
        assertThat(
                        registry.get("tinyroute.links.duration")
                                .tags("route", "redirect", "method", "HEAD", "status", "302")
                                .timer()
                                .count())
                .isEqualTo(1);
    }
}
