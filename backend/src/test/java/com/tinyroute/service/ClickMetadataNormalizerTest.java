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
    void recognizesAdvertisedBraveOnIosBeforeSafari() {
        for (String brand : new String[] {"Brave", "Brave/1"}) {
            var metadata =
                    normalizer.normalize(
                            null,
                            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) "
                                    + "AppleWebKit/605.1.15 Version/26.5 Mobile/15E148 Safari/604.1 "
                                    + brand);
            assertThat(metadata.browser()).isEqualTo("Brave");
            assertThat(metadata.operatingSystem()).isEqualTo("iOS");
            assertThat(metadata.device()).isEqualTo("Mobile");
        }
    }

    @Test
    void usesAdvertisedBraveBrandRegardlessOfOrderWithoutKeepingVersionsOrRawHints() {
        for (String hints :
                new String[] {
                    "\"Brave\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"",
                    "\"Not,A;Brand\";v=\"99\", \"Chromium\";v=\"137\", \"Brave\";v=\"137\"",
                    " \t\"Chromium\";v=\"137\" , \"Brave\" ; v=\"137\" \t"
                }) {
            var metadata =
                    normalizer.normalize(
                            null,
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Chrome/137.0.0.0 Safari/537.36",
                            hints);
            assertThat(metadata.browser()).isEqualTo("Brave");
            assertThat(metadata.operatingSystem()).isEqualTo("macOS");
            assertThat(metadata.device()).isEqualTo("Desktop");
            assertThat(metadata.toString()).doesNotContain("137", "Chromium", "Not,A", "Mozilla");
        }
    }

    @Test
    void fallsBackToUserAgentForMissingMalformedOversizedOrUnrecognizedBrands() {
        for (String hints :
                new String[] {
                    null,
                    "",
                    "\"Chromium\";v=\"137\", \"Google Chrome\";v=\"137\"",
                    "\"Not Brave\";v=\"137\"",
                    "\"Brave Browser\";v=\"137\"",
                    "\"Not, \\\"Brave\\\";v=\\\"137\\\"\";v=\"99\"",
                    "Brave",
                    "\"Brave\";v=137",
                    "\"Brave\";v=\"137\",",
                    "\"Brave\";v=\"137\", malformed",
                    "\"Brave\";v=\"137\"\r\n",
                    "\"Brave\";v=\"137\"" + " ".repeat(1024)
                }) {
            assertThat(normalizer.normalize(null, "Chrome/137 Safari/537", hints).browser())
                    .isEqualTo("Chrome");
            assertThat(normalizer.normalize(null, "Chrome/137 Edg/137", hints).browser())
                    .isEqualTo("Edge");
            assertThat(normalizer.normalize(null, "Safari/604", hints).browser())
                    .isEqualTo("Safari");
        }
        assertThat(normalizer.normalize(null, "NotBrave Safari/604").browser()).isEqualTo("Safari");
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
