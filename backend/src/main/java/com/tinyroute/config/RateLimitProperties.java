package com.tinyroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("tinyroute.rate-limit")
public class RateLimitProperties {

    private String hmacSecret;
    private List<String> trustedProxyCidrs = new ArrayList<>();

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
        this.trustedProxyCidrs = trustedProxyCidrs == null ? new ArrayList<>() : new ArrayList<>(trustedProxyCidrs);
    }
}
