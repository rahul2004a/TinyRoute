package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tinyroute.config.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RedirectPageRendererTest {
    @ParameterizedTest
    @CsvSource({
        "403, Link unavailable",
        "404, Link not found",
        "429, Too many requests",
        "503, Service temporarily unavailable"
    })
    void publicErrorsOfferExplicitNavigationToTheConfiguredFrontend(int status, String title) {
        var security = new SecurityProperties();
        security.setAllowedOrigins(
                List.of("https://app.tinyroute.test:3000", "https://preview.tinyroute.test"));

        var html = new RedirectPageRenderer(security).render(status);

        assertThat(html)
                .contains("<title>" + title + " · TinyRoute</title>")
                .contains("<h1 id=\"error-title\">" + title + "</h1>")
                .contains("<a class=\"brand\" href=\"https://app.tinyroute.test:3000\"")
                .contains("<a class=\"home\" href=\"https://app.tinyroute.test:3000\"")
                .contains("Back to TinyRoute")
                .contains("<meta name=\"robots\" content=\"noindex,nofollow\">")
                .doesNotContain("preview.tinyroute.test", "<script", "http-equiv=\"refresh\"");
    }

    @Test
    void unsupportedStatusesCannotProduceAnErrorDocument() {
        var security = new SecurityProperties();
        security.setAllowedOrigins(List.of("https://app.tinyroute.test"));
        var renderer = new RedirectPageRenderer(security);

        assertThatThrownBy(() -> renderer.render(302)).isInstanceOf(IllegalArgumentException.class);
    }
}
