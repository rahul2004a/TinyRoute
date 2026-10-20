package com.tinyroute.model;

/** Normalized categories only; an empty referrer means Direct / unknown. */
public record ClickMetadata(
        String referrer,
        String device,
        String operatingSystem,
        String browser,
        String country,
        String city) {
    public ClickMetadata withLocation(String country, String city) {
        return new ClickMetadata(referrer, device, operatingSystem, browser, country, city);
    }
}
