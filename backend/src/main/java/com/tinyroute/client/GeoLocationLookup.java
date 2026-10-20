package com.tinyroute.client;

import java.net.InetAddress;

public interface GeoLocationLookup {
    Location lookup(InetAddress address);

    record Location(String country, String city) {
        public static final Location UNKNOWN = new Location(null, null);
    }
}
