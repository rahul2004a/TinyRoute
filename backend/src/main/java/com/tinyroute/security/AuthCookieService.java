package com.tinyroute.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "__Host-tinyroute_access";
    public static final String REFRESH_COOKIE_NAME = "__Host-tinyroute_refresh";

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    public ResponseCookie accessCookie(String token) {
        return authCookie(ACCESS_COOKIE_NAME, requireToken(token), ACCESS_TOKEN_TTL);
    }

    public ResponseCookie refreshCookie(String token) {
        return authCookie(REFRESH_COOKIE_NAME, requireToken(token), REFRESH_TOKEN_TTL);
    }

    public ResponseCookie clearAccessCookie() {
        return authCookie(ACCESS_COOKIE_NAME, "", Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return authCookie(REFRESH_COOKIE_NAME, "", Duration.ZERO);
    }

    private ResponseCookie authCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String requireToken(String token) {
        if (Objects.requireNonNull(token, "token must not be null").isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return token;
    }
}
