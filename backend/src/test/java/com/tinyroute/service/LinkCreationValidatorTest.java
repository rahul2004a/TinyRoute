package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.exception.LinkValidationException;
import com.tinyroute.model.CreateLinkCommand;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LinkCreationValidatorTest {
    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
    private final LinkCreationValidator validator = new LinkCreationValidator(properties());

    private static LinkProperties properties() {
        var properties = new LinkProperties();
        properties.setShortBaseUrl("https://go.tinyroute.test");
        return properties;
    }

    @Test
    void optionalExpiryAndAliasStayAbsent() {
        var input =
                validator.validate(new CreateLinkCommand("https://example.com", null, null), NOW);
        assertThat(input.expiresAt()).isNull();
        assertThat(input.alias()).isNull();
    }

    @Test
    void convertsExplicitOffsetsToTheSameMillisecondInstant() {
        var input =
                validator.validate(
                        new CreateLinkCommand(
                                "https://example.com", "Abc", "2026-10-07T14:00:01.123+02:00"),
                        NOW);
        assertThat(input.expiresAt()).isEqualTo(Instant.parse("2026-10-07T12:00:01.123Z"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "2026-10-08",
                "2026-10-08T12:00:00",
                "2026-10-07T12:00:00Z",
                "2026-10-07T11:59:59Z",
                "2026-10-08T12:00:00.1234Z",
                "+10000-01-01T00:00:00Z",
                "2026-02-30T00:00:00Z",
                "0000-01-01T00:00:00Z"
            })
    void rejectsInvalidOrNonfutureExpiry(String expiry) {
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        new CreateLinkCommand("https://example.com", null, expiry),
                                        NOW))
                .isInstanceOf(LinkValidationException.class);
    }

    @Test
    void rechecksExpiryAfterAnOwnerLockWait() {
        Instant expiry = NOW.plusSeconds(1);
        assertThatThrownBy(() -> validator.requireFutureExpiry(expiry, expiry))
                .isInstanceOf(LinkValidationException.class);
    }
}
