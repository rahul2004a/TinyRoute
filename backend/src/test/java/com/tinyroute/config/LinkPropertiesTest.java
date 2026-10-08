package com.tinyroute.config;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.model.ShortCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LinkPropertiesTest {
    @Test
    void buildsShortUrlsOnlyFromTheValidatedConfiguredOrigin() {
        var p = new LinkProperties();
        p.setShortBaseUrl("https://go.tinyroute.test:8443/");
        assertThat(p.shortUrl(ShortCode.forAlias("Abc")))
                .isEqualTo("https://go.tinyroute.test:8443/Abc");
        assertThat(p.shortHost()).isEqualTo("go.tinyroute.test");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://go.test",
                "https://user@go.test",
                "https://go.test/path",
                "https://go.test?x=1",
                "https://go.test#x",
                "https://go.test:0",
                "",
                "https://127.1"
            })
    void refusesInvalidShortOrigins(String origin) {
        assertThatThrownBy(() -> new LinkProperties().setShortBaseUrl(origin))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
