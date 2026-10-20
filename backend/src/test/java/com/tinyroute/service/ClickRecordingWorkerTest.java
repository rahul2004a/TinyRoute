package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tinyroute.client.GeoLocationLookup;
import com.tinyroute.model.ClickMetadata;
import com.tinyroute.repository.ClickEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import org.junit.jupiter.api.Test;

class ClickRecordingWorkerTest {
    private final ClickEventRepository events = mock(ClickEventRepository.class);
    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final ClickMetadata metadata =
            new ClickMetadata("", "Unknown", "Unknown", "Unknown", null, null);

    @Test
    void staleObservationsNeverReachStorage() {
        var worker =
                new ClickRecordingWorker(
                        events, address -> GeoLocationLookup.Location.UNKNOWN, clock, metrics);
        worker.record("abc", clock.instant().minusSeconds(46), metadata, null);
        verifyNoInteractions(events);
        assertThat(metrics.get("tinyroute.analytics.stale").counter().count()).isEqualTo(1);
    }

    @Test
    void geographyFailureStillRecordsUnknownAndPersistenceFailureNeverEscapes() {
        var worker =
                new ClickRecordingWorker(
                        events,
                        address -> {
                            throw new IllegalStateException("private address");
                        },
                        clock,
                        metrics);
        worker.record("abc", clock.instant(), metadata, null);
        verify(events).record("abc", clock.instant(), metadata);
        assertThat(metrics.get("tinyroute.analytics.recorded").counter().count()).isEqualTo(1);
        doThrow(new IllegalStateException("private database failure"))
                .when(events)
                .record(anyString(), any(), any());
        assertThatCode(() -> worker.record("abc", clock.instant(), metadata, null))
                .doesNotThrowAnyException();
        assertThat(metrics.get("tinyroute.analytics.failed").counter().count()).isEqualTo(1);
    }

    @Test
    void rejectedSubmissionDoesNotEscapeToRedirect() {
        var worker = mock(ClickRecordingWorker.class);
        doThrow(new org.springframework.core.task.TaskRejectedException("full"))
                .when(worker)
                .record(anyString(), any(), any(), any());
        var service = new ClickCountService(worker, metrics);
        assertThatCode(
                        () ->
                                service.recordSuccessfulRedirect(
                                        "abc", clock.instant(), metadata, null))
                .doesNotThrowAnyException();
        assertThat(metrics.get("tinyroute.analytics.rejected").counter().count()).isEqualTo(1);
    }
}
