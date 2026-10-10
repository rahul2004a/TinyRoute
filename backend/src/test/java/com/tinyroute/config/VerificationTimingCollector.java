package com.tinyroute.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.tinyroute.security.LinkRequestTimingFilter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Bounded, test-only capture of fixed operational timing records; no request inputs. */
public final class VerificationTimingCollector implements AutoCloseable {
    public record Sample(String route, int status, long durationNanos) {}

    public record Readout(List<Sample> samples, boolean overflowed) {}

    private final List<Sample> samples = new ArrayList<>();
    private boolean overflowed;
    private final Logger logger = (Logger) LoggerFactory.getLogger(LinkRequestTimingFilter.class);
    private final AppenderBase<ILoggingEvent> appender =
            new AppenderBase<>() {
                @Override
                protected void append(ILoggingEvent event) {
                    Object[] args = event.getArgumentArray();
                    if (args == null
                            || args.length != 4
                            || !(args[0] instanceof String route)
                            || !java.util.Set.of("create", "redirect").contains(route)
                            || !(args[2] instanceof Integer status)
                            || !(args[3] instanceof Long duration)) return;
                    capture(new Sample(route, status, duration));
                }
            };

    public VerificationTimingCollector() {
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
    }

    private synchronized void capture(Sample sample) {
        if (samples.size() >= 100_000) {
            overflowed = true;
            return;
        }
        samples.add(sample);
    }

    public synchronized void reset() {
        samples.clear();
        overflowed = false;
    }

    public synchronized Readout readout() {
        return new Readout(List.copyOf(samples), overflowed);
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
