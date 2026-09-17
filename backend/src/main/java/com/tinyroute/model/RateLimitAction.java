package com.tinyroute.model;

import java.time.Duration;

public enum RateLimitAction {
    REGISTER("register", 5, Duration.ofMinutes(15)),
    PASSWORD_LOGIN("password-login", 5, Duration.ofMinutes(15)),
    GOOGLE_START("google-start", 5, Duration.ofMinutes(15)),
    PASSWORD_RESET_REQUEST("password-reset-request", 5, Duration.ofMinutes(15)),
    OTP_VERIFY_CLIENT("otp-verify-client", 10, Duration.ofMinutes(15)),
    OTP_VERIFY_PENDING_REGISTRATION("otp-verify-pending-registration", 5, Duration.ofMinutes(15)),
    OTP_RESEND_PENDING_REGISTRATION("otp-resend-pending-registration", 3, Duration.ofHours(1)),
    REFRESH_SESSION_FAMILY("refresh-session-family", 30, Duration.ofMinutes(15)),
    RESET_CONFIRM_CLIENT("reset-confirm-client", 10, Duration.ofMinutes(15)),
    RESET_CONFIRM_TOKEN("reset-confirm-token", 5, Duration.ofMinutes(15));

    private final String keySegment;
    private final int maximumAttempts;
    private final Duration window;

    RateLimitAction(String keySegment, int maximumAttempts, Duration window) {
        this.keySegment = keySegment;
        this.maximumAttempts = maximumAttempts;
        this.window = window;
    }

    public String keySegment() {
        return keySegment;
    }

    public int maximumAttempts() {
        return maximumAttempts;
    }

    public Duration window() {
        return window;
    }
}
