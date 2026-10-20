package com.tinyroute.service;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.dto.AnalyticsResponse;
import com.tinyroute.dto.AnalyticsResponse.*;
import com.tinyroute.exception.*;
import com.tinyroute.model.*;
import com.tinyroute.repository.ClickEventRepository;
import com.tinyroute.security.OwnershipGuard;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.util.MultiValueMap;

@Service
public class AnalyticsService {
    private final ClickEventRepository events;
    private final OwnershipGuard ownership;
    private final Clock clock;
    private final LinkProperties links;

    public AnalyticsService(
            ClickEventRepository events,
            OwnershipGuard ownership,
            Clock clock,
            LinkProperties links) {
        this.events = events;
        this.ownership = ownership;
        this.clock = clock;
        this.links = links;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public AnalyticsResponse overview(
            AccessToken principal, MultiValueMap<String, String> parameters) {
        if (principal == null) throw new AuthenticationFailedException();
        return snapshot(principal.userId(), null, parameters);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public AnalyticsResponse detail(
            AccessToken principal, String id, MultiValueMap<String, String> parameters) {
        if (principal == null) throw new AuthenticationFailedException();
        return snapshot(
                principal.userId(), ownership.requireOwned(id, principal.userId()), parameters);
    }

    private AnalyticsResponse snapshot(
            UUID owner, Link link, MultiValueMap<String, String> parameters) {
        for (String field : List.of("from", "to"))
            if (parameters.containsKey(field) && parameters.get(field).size() != 1)
                throw new LinkValidationException(field, "Provide this date once.");
        var range =
                AnalyticsRange.parse(parameters.getFirst("from"), parameters.getFirst("to"), clock);
        UUID id = link == null ? null : link.id();
        var counts = new HashMap<LocalDate, Long>();
        for (var day : events.daily(owner, id, range.start(), range.endExclusive()))
            counts.put(day.getDate(), day.getCount());
        var daily =
                range.from()
                        .datesUntil(range.to().plusDays(1))
                        .map(day -> new Day(day, counts.getOrDefault(day, 0L)))
                        .toList();
        Map<String, List<Bucket>> breakdowns = new LinkedHashMap<>();
        Geography geography = null;
        for (String dimension :
                List.of(
                        "referrers",
                        "devices",
                        "operatingSystems",
                        "browsers",
                        "countries",
                        "cities")) {
            var rows = events.breakdown(owner, id, range.start(), range.endExclusive(), dimension);
            if (link != null && dimension.equals("countries")) {
                var countries =
                        rows.stream()
                                .filter(row -> row.getKind().equals("VALUE"))
                                .map(
                                        row ->
                                                new Country(
                                                        row.getLabel(),
                                                        countryLabel(
                                                                dimension,
                                                                row.getKind(),
                                                                row.getLabel()),
                                                        row.getCount()))
                                .sorted(
                                        Comparator.comparingLong(Country::count)
                                                .reversed()
                                                .thenComparing(Country::label))
                                .toList();
                long unknown =
                        rows.stream()
                                .filter(row -> !row.getKind().equals("VALUE"))
                                .mapToLong(row -> row.getCount())
                                .sum();
                geography = new Geography(countries, unknown);
            }
            var buckets =
                    rows.stream()
                            .map(
                                    row ->
                                            new Bucket(
                                                    row.getKind(),
                                                    countryLabel(
                                                            dimension,
                                                            row.getKind(),
                                                            row.getLabel()),
                                                    row.getCount()))
                            .toList();
            if (dimension.equals("countries")) {
                var known =
                        buckets.stream()
                                .filter(bucket -> bucket.kind().equals("VALUE"))
                                .sorted(
                                        Comparator.comparingLong(Bucket::count)
                                                .reversed()
                                                .thenComparing(Bucket::label))
                                .toList();
                var bounded = new ArrayList<>(known.stream().limit(20).toList());
                buckets.stream()
                        .filter(bucket -> !bucket.kind().equals("VALUE"))
                        .forEach(bounded::add);
                long remainder = known.stream().skip(20).mapToLong(Bucket::count).sum();
                if (remainder > 0) bounded.add(new Bucket("OTHER", "Other", remainder));
                bounded.sort(
                        Comparator.comparingLong(Bucket::count)
                                .reversed()
                                .thenComparing(Bucket::label));
                buckets = List.copyOf(bounded);
            }
            breakdowns.put(dimension, buckets);
        }
        String status =
                link == null
                        ? null
                        : link.status() == LinkStatus.ACTIVE
                                        && link.expiresAt() != null
                                        && !link.expiresAt().isAfter(clock.instant())
                                ? "EXPIRED"
                                : link.status().name();
        var details =
                link == null
                        ? null
                        : new LinkDetails(
                                link.id(),
                                link.code(),
                                links.shortUrl(new ShortCode(link.code())),
                                link.destinationUrl(),
                                status);
        return new AnalyticsResponse(
                new Range(range.from(), range.to(), "UTC"),
                clock.instant(),
                60,
                daily.stream().mapToLong(Day::count).sum(),
                events.lifetime(owner, id),
                daily,
                breakdowns,
                geography,
                details);
    }

    private String countryLabel(String dimension, String kind, String label) {
        return dimension.equals("countries") && kind.equals("VALUE")
                ? new Locale.Builder().setRegion(label).build().getDisplayCountry(Locale.ENGLISH)
                : label;
    }
}
