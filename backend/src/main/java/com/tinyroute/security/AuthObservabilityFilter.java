package com.tinyroute.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthObservabilityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthObservabilityFilter.class);
    private final MeterRegistry meterRegistry;

    public AuthObservabilityFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Route route = routeFor(request.getRequestURI());
        String method = safeMethod(request.getMethod());
        long started = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            int status = failed ? 500 : response.getStatus();
            long durationNanos = System.nanoTime() - started;
            String statusTag = Integer.toString(status);
            Counter.builder("tinyroute.auth.requests")
                    .tag("route", route.name())
                    .tag("status", statusTag)
                    .register(meterRegistry)
                    .increment();
            Timer.builder("tinyroute.auth.duration")
                    .tag("route", route.name())
                    .tag("status", statusTag)
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
            log.info("{\"method\":\"{}\",\"path\":\"{}\",\"status\":{},\"durationMs\":{}}",
                    method, route.path(), status, TimeUnit.NANOSECONDS.toMillis(durationNanos));
        }
    }

    private static String safeMethod(String method) {
        return switch (method) {
            case "GET", "POST", "DELETE" -> method;
            default -> "OTHER";
        };
    }

    private static Route routeFor(String path) {
        return switch (path) {
            case "/api/auth/csrf" -> new Route("csrf", path);
            case "/api/auth/register" -> new Route("register", path);
            case "/api/auth/register/verify" -> new Route("verify", path);
            case "/api/auth/register/resend-otp" -> new Route("resend", path);
            case "/api/auth/login" -> new Route("login", path);
            case "/api/auth/me" -> new Route("me", path);
            case "/api/auth/google/start" -> new Route("google-start", path);
            case "/api/auth/google/callback" -> new Route("google-callback", path);
            case "/api/auth/refresh" -> new Route("refresh", path);
            case "/api/auth/logout" -> new Route("logout", path);
            case "/api/auth/password-reset" -> new Route("password-reset", path);
            case "/api/auth/password-reset/confirm" -> new Route("reset-confirm", path);
            case "/api/auth/account" -> new Route("account", path);
            default -> new Route("unmatched", "/api/auth/*");
        };
    }

    private record Route(String name, String path) {}
}
