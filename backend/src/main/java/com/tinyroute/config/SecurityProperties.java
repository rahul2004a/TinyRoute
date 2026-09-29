package com.tinyroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("tinyroute.security")
public class SecurityProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return List.copyOf(allowedOrigins);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = new ArrayList<>(allowedOrigins);
    }

    public List<String> allowedOrigins() {
        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed frontend origin is required");
        }
        allowedOrigins.forEach(this::validateOrigin);
        return List.copyOf(allowedOrigins);
    }

    private void validateOrigin(String origin) {
        URI uri = URI.create(origin);
        if (!"https".equals(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getPort() < -1
                || !"".equals(uri.getPath())
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !origin.equals(uri.toString())) {
            throw new IllegalArgumentException("Allowed frontend origins must be exact HTTPS origins");
        }
    }
}
