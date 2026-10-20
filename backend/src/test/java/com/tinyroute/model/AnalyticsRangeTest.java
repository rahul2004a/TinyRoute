package com.tinyroute.model;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.exception.LinkValidationException;
import java.time.*;
import org.junit.jupiter.api.Test;

class AnalyticsRangeTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T23:45:00Z"), ZoneOffset.UTC);

    @Test
    void defaultsToThirtyInclusiveUtcDates() {
        var range = AnalyticsRange.parse(null, null, clock);
        assertThat(range.from()).isEqualTo(LocalDate.parse("2026-09-21"));
        assertThat(range.to()).isEqualTo(LocalDate.parse("2026-10-20"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2026-10-21T00:00:00Z"));
    }

    @Test
    void acceptsLeapDayAndSingleDay() {
        var range = AnalyticsRange.parse("2024-02-29", "2024-02-29", clock);
        assertThat(range.start()).isEqualTo(Instant.parse("2024-02-29T00:00:00Z"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2024-03-01T00:00:00Z"));
    }

    @Test
    void rejectsPartialMalformedReversedFutureAndOversizedRanges() {
        for (String[] pair :
                new String[][] {
                    {null, "2026-10-20"},
                    {"", "2026-10-20"},
                    {"2026-2-01", "2026-10-20"},
                    {"2026-02-30", "2026-10-20"},
                    {"2026-10-20", "2026-10-19"},
                    {"2026-10-20", "2026-10-21"},
                    {"2024-01-01", "2026-10-20"}
                }) {
            assertThatThrownBy(() -> AnalyticsRange.parse(pair[0], pair[1], clock))
                    .isInstanceOf(LinkValidationException.class);
        }
    }
}
