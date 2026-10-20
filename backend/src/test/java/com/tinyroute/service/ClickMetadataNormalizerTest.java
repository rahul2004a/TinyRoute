package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClickMetadataNormalizerTest {
    private final ClickMetadataNormalizer normalizer = new ClickMetadataNormalizer();

    @Test
    void removesReferrerPathQueryAndCredentials() {
        var metadata =
                normalizer.normalize(
                        "https://user:secret@EXAMPLE.com:8443/private?token=secret#x",
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1 Safari/604.1 Mobile");
        assertThat(metadata.referrer()).isEqualTo("example.com");
        assertThat(metadata.device()).isEqualTo("Mobile");
        assertThat(metadata.operatingSystem()).isEqualTo("iOS");
        assertThat(metadata.browser()).isEqualTo("Safari");
        assertThat(metadata.toString()).doesNotContain("secret", "private", "Mozilla");
    }

    @Test
    void recognizesSpecificBrowsersBeforeGenericTokens() {
        var metadata =
                normalizer.normalize(
                        null,
                        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537 Chrome/123 Safari/537 Edg/123");
        assertThat(metadata.referrer()).isEmpty();
        assertThat(metadata.browser()).isEqualTo("Edge");
        assertThat(metadata.device()).isEqualTo("Desktop");
        assertThat(metadata.operatingSystem()).isEqualTo("Windows");
    }

    @Test
    void rejectsUnsafeReferrersAndUnrecognizedOrOversizedAgents() {
        for (String referrer :
                new String[] {
                    "https://127.0.0.1/path",
                    "https://[::1]/",
                    "javascript:secret",
                    "https://localhost/",
                    "bad",
                    "https://" + "a".repeat(2050)
                }) assertThat(normalizer.normalize(referrer, "x".repeat(1025)).referrer()).isNull();
        assertThat(normalizer.normalize(null, "unrecognized").device()).isEqualTo("Unknown");
        assertThat(normalizer.normalize(null, null).browser()).isEqualTo("Unknown");
    }
}
