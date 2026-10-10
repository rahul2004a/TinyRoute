package com.tinyroute.security;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Operational request latency only; no link analytics or user-supplied labels (NFR-PER-01/02). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class LinkRequestTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(LinkRequestTimingFilter.class);
    private final MeterRegistry registry;
    private final PublicRedirectRequestMatcher publicRedirects;

    public LinkRequestTimingFilter(
            MeterRegistry registry, PublicRedirectRequestMatcher publicRedirects) {
        this.registry = registry;
        this.publicRedirects = publicRedirects;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isCreation(request)
                && !(publicRedirects.matches(request)
                        && request.getRequestURI().indexOf('/', 1) < 0);
    }

    private boolean isCreation(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "/api/links".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String route = isCreation(request) ? "create" : "redirect";
        String method =
                isCreation(request)
                        ? "POST"
                        : ("HEAD".equals(request.getMethod()) ? "HEAD" : "GET");
        long started = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException failure) {
            failed = true;
            throw failure;
        } finally {
            int status = failed ? 500 : response.getStatus();
            long durationNanos = System.nanoTime() - started;
            Timer.builder("tinyroute.links.duration")
                    .tag("route", route)
                    .tag("method", method)
                    .tag("status", Integer.toString(status))
                    .register(registry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
            log.info(
                    "link_request route={} method={} status={} durationNanos={}",
                    route,
                    method,
                    status,
                    durationNanos);
        }
    }
}
