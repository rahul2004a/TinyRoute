package com.tinyroute.service;

import com.tinyroute.model.ClickMetadata;
import io.micrometer.core.instrument.*;
import java.net.InetAddress;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ClickCountService {
    private final ClickRecordingWorker worker;
    private final Counter rejected;

    public ClickCountService(ClickRecordingWorker worker, MeterRegistry metrics) {
        this.worker = worker;
        rejected = metrics.counter("tinyroute.analytics.rejected");
    }

    public void recordSuccessfulRedirect(
            String code, Instant issuedAt, ClickMetadata metadata, InetAddress address) {
        try {
            worker.record(code, issuedAt, metadata, address);
        } catch (RuntimeException overloaded) {
            rejected.increment();
        }
    }
}
