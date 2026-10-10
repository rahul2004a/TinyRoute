package com.tinyroute.service;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.exception.LinkValidationException;
import com.tinyroute.model.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class LinkCreationValidator {
    private final LinkProperties properties;

    public LinkCreationValidator(LinkProperties properties) {
        this.properties = properties;
    }

    public ValidatedLinkInput validate(CreateLinkCommand input, Instant now) {
        var destination = DestinationUrl.parse(input.destinationUrl(), properties.shortHost());
        var alias = input.alias() == null ? null : ShortCode.forAlias(input.alias());
        Instant expiry = null;
        if (input.expiresAt() != null) {
            String value = input.expiresAt();
            if (!value.matches(
                    "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{1,3})?(?:Z|[+-][0-9]{2}:[0-9]{2})"))
                throw invalidExpiry();
            try {
                expiry = OffsetDateTime.parse(value).toInstant();
                if (expiry.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                        || !expiry.isBefore(Instant.parse("+10000-01-01T00:00:00Z")))
                    throw invalidExpiry();
            } catch (DateTimeParseException exception) {
                throw invalidExpiry();
            }
            requireFutureExpiry(expiry, now);
        }
        return new ValidatedLinkInput(destination, alias, expiry);
    }

    public void requireFutureExpiry(Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) throw invalidExpiry();
    }

    private LinkValidationException invalidExpiry() {
        return new LinkValidationException(
                "expiresAt",
                "Choose a future date and time with an explicit timezone and millisecond precision or less.");
    }
}
