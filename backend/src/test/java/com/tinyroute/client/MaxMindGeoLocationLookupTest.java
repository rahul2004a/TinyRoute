package com.tinyroute.client;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaxMindGeoLocationLookupTest {
    @Test
    void looksUpSyntheticCityWithoutAnExternalService() throws Exception {
        String fixture =
                Path.of(getClass().getResource("/geo/GeoIP2-City-Test.mmdb").toURI()).toString();
        var lookup = new MaxMindGeoLocationLookup(fixture);
        try {
            var location = lookup.lookup(InetAddress.getByName("89.160.20.128"));
            assertThat(location.country()).isEqualTo("SE");
            assertThat(location.city()).isEqualTo("Linköping, SE");
            assertThat(lookup.lookup(InetAddress.getLoopbackAddress()))
                    .isEqualTo(GeoLocationLookup.Location.UNKNOWN);
        } finally {
            lookup.close();
        }
    }

    @Test
    void missingConfigurationReturnsUnknownAndBadConfigurationFailsSafely() throws Exception {
        var lookup = new MaxMindGeoLocationLookup("");
        assertThat(lookup.lookup(InetAddress.getByName("89.160.20.128")))
                .isEqualTo(GeoLocationLookup.Location.UNKNOWN);
        lookup.close();
        assertThatThrownBy(() -> new MaxMindGeoLocationLookup("/not-a-real-path/private.mmdb"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("private.mmdb");
    }
}
