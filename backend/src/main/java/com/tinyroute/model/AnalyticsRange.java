package com.tinyroute.model;

import com.tinyroute.exception.LinkValidationException;
import java.time.*;
import java.time.temporal.ChronoUnit;

public record AnalyticsRange(LocalDate from, LocalDate to) {
    public static AnalyticsRange parse(String from, String to, Clock clock) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (from == null && to == null) return new AnalyticsRange(today.minusDays(29), today);
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");
        if (start.isAfter(end))
            throw new LinkValidationException("to", "End date must be on or after the start date.");
        if (end.isAfter(today))
            throw new LinkValidationException("to", "End date cannot be in the future (UTC).");
        if (ChronoUnit.DAYS.between(start, end) >= 366)
            throw new LinkValidationException("from", "Choose at most 366 days.");
        return new AnalyticsRange(start, end);
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
                throw new IllegalArgumentException();
            var date = LocalDate.parse(value);
            if (date.getYear() < 1) throw new IllegalArgumentException();
            return date;
        } catch (IllegalArgumentException | DateTimeException invalid) {
            throw new LinkValidationException(field, "Choose a valid date in YYYY-MM-DD format.");
        }
    }

    public Instant start() {
        return from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Instant endExclusive() {
        return to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
