package com.tinyroute.client;

import com.maxmind.geoip2.DatabaseReader;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.InetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaxMindGeoLocationLookup implements GeoLocationLookup {
    private final DatabaseReader reader;

    public MaxMindGeoLocationLookup(
            @Value("${tinyroute.analytics.geo-database-path:}") String path) {
        try {
            reader = path.isBlank() ? null : new DatabaseReader.Builder(new File(path)).build();
        } catch (IOException | RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Analytics geography database configuration is invalid.");
        }
    }

    @Override
    public Location lookup(InetAddress address) {
        if (reader == null
                || address == null
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) return Location.UNKNOWN;
        try {
            var result = reader.tryCity(address);
            if (result.isEmpty()) return Location.UNKNOWN;
            String country = result.get().country().isoCode();
            if (country == null || !country.matches("[A-Z]{2}")) return Location.UNKNOWN;
            String city = result.get().city().name();
            if (city != null
                    && (city.isBlank()
                            || city.length() > 150
                            || city.chars().anyMatch(Character::isISOControl))) city = null;
            return new Location(country, city == null ? null : city + ", " + country);
        } catch (IOException
                | com.maxmind.geoip2.exception.GeoIp2Exception
                | RuntimeException failure) {
            return Location.UNKNOWN;
        }
    }

    @PreDestroy
    public void close() throws IOException {
        if (reader != null) reader.close();
    }
}
