package com.tinyroute.service;

import com.tinyroute.client.GeoLocationLookup;
import com.tinyroute.model.ClickMetadata;
import com.tinyroute.repository.ClickEventRepository;
import io.micrometer.core.instrument.*;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ClickRecordingWorker {
    private final ClickEventRepository events;
    private final GeoLocationLookup geography;
    private final Clock clock;
    private final Counter recorded, failed, stale;
    private final Timer recordingDelay;

    public ClickRecordingWorker(
            ClickEventRepository events,
            GeoLocationLookup geography,
            Clock clock,
            MeterRegistry metrics) {
        this.events = events;
        this.geography = geography;
        this.clock = clock;
        recorded = metrics.counter("tinyroute.analytics.recorded");
        failed = metrics.counter("tinyroute.analytics.failed");
        stale = metrics.counter("tinyroute.analytics.stale");
        recordingDelay = metrics.timer("tinyroute.analytics.recording.delay");
    }

    @Async("analyticsExecutor")
    public void record(String code, Instant issuedAt, ClickMetadata metadata, InetAddress address) {
        try {
            if (issuedAt.isBefore(clock.instant().minusSeconds(45))) {
                stale.increment();
                return;
            }
            GeoLocationLookup.Location location;
            try {
                location = geography.lookup(address);
            } catch (RuntimeException unavailable) {
                location = GeoLocationLookup.Location.UNKNOWN;
            }
            events.record(
                    code, issuedAt, metadata.withLocation(location.country(), location.city()));
            recordingDelay.record(java.time.Duration.between(issuedAt, clock.instant()));
            recorded.increment();
        } catch (RuntimeException failure) {
            // Never log async arguments or the failure: either can contain transient request data.
            failed.increment();
        }
    }
}
