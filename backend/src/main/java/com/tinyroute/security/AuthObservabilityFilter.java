package com.tinyroute.security;

import io.micrometer.core.instrument.Counter;
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
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
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
            log.info(
                    "{\"method\":\"{}\",\"path\":\"{}\",\"status\":{},\"durationMs\":{}}",
                    method,
                    route.path(),
                    status,
                    TimeUnit.NANOSECONDS.toMillis(durationNanos));
        }
    }

    private static String safeMethod(String method) {
        return switch (method) {
            case "GET" -> "GET";
            case "POST" -> "POST";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private static Route routeFor(String path) {
        return switch (path) {
            case "/api/auth/csrf" -> new Route("csrf", "/api/auth/csrf");
            case "/api/auth/register" -> new Route("register", "/api/auth/register");
            case "/api/auth/register/verify" -> new Route("verify", "/api/auth/register/verify");
            case "/api/auth/register/resend-otp" ->
                    new Route("resend", "/api/auth/register/resend-otp");
            case "/api/auth/login" -> new Route("login", "/api/auth/login");
            case "/api/auth/me" -> new Route("me", "/api/auth/me");
            case "/api/auth/google/start" -> new Route("google-start", "/api/auth/google/start");
            case "/api/auth/google/callback" ->
                    new Route("google-callback", "/api/auth/google/callback");
            case "/api/auth/refresh" -> new Route("refresh", "/api/auth/refresh");
            case "/api/auth/logout" -> new Route("logout", "/api/auth/logout");
            case "/api/auth/password-reset" ->
                    new Route("password-reset", "/api/auth/password-reset");
            case "/api/auth/password-reset/confirm" ->
                    new Route("reset-confirm", "/api/auth/password-reset/confirm");
            case "/api/auth/account" -> new Route("account", "/api/auth/account");
            default -> new Route("unmatched", "/api/auth/*");
        };
    }

    private record Route(String name, String path) {}
}
