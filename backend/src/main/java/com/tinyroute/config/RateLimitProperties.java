package com.tinyroute.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tinyroute.rate-limit")
public class RateLimitProperties {

    private String hmacSecret;
    private List<String> trustedProxyCidrs = new ArrayList<>();
    private int creationMaximumAttempts = 100;
    private Duration creationWindow = Duration.ofHours(1);
    private int redirectMaximumAttempts = 600;
    private Duration redirectWindow = Duration.ofMinutes(1);

    public int getCreationMaximumAttempts() {
        return creationMaximumAttempts;
    }

    public void setCreationMaximumAttempts(int value) {
        requirePositive(value);
        creationMaximumAttempts = value;
    }

    public Duration getCreationWindow() {
        return creationWindow;
    }

    public void setCreationWindow(Duration value) {
        requireWindow(value);
        creationWindow = value;
    }

    public int getRedirectMaximumAttempts() {
        return redirectMaximumAttempts;
    }

    public void setRedirectMaximumAttempts(int value) {
        requirePositive(value);
        redirectMaximumAttempts = value;
    }

    public Duration getRedirectWindow() {
        return redirectWindow;
    }

    public void setRedirectWindow(Duration value) {
        requireWindow(value);
        if (value.compareTo(Duration.ofHours(24)) > 0)
            throw new IllegalArgumentException("Redirect window exceeds privacy retention ceiling");
        redirectWindow = value;
    }

    private void requirePositive(int value) {
        if (value < 1) throw new IllegalArgumentException("Positive attempt budget required");
    }

    private void requireWindow(Duration value) {
        if (value == null || value.isNegative() || value.toMillis() < 1)
            throw new IllegalArgumentException("Positive millisecond window required");
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public List<String> getTrustedProxyCidrs() {
        return List.copyOf(trustedProxyCidrs);
    }

    public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) {
        this.trustedProxyCidrs =
                trustedProxyCidrs == null ? new ArrayList<>() : new ArrayList<>(trustedProxyCidrs);
    }
}
