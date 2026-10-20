package com.tinyroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.*;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyticsResponse(
        Range range,
        Instant generatedAt,
        int recordingDelaySeconds,
        long totalClicks,
        long lifetimeClicks,
        List<Day> dailyClicks,
        Map<String, List<Bucket>> breakdowns,
        Geography geography,
        LinkDetails link) {
    public record Range(LocalDate from, LocalDate to, String timeZone) {}

    public record Day(LocalDate date, long count) {}

    public record Bucket(String kind, String label, long count) {}

    public record Country(String countryCode, String label, long count) {}

    public record Geography(List<Country> countries, long unknownClicks) {}

    public record LinkDetails(
            UUID id, String code, String shortUrl, String destinationUrl, String status) {}
}
