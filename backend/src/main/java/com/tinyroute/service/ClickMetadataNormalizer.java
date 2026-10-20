package com.tinyroute.service;

import com.tinyroute.model.ClickMetadata;
import java.net.*;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ClickMetadataNormalizer {
    public ClickMetadata normalize(String referer, String userAgent) {
        String ua =
                userAgent == null || userAgent.length() > 1024
                        ? ""
                        : userAgent.toLowerCase(Locale.ROOT);
        String os =
                ua.contains("android")
                        ? "Android"
                        : ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")
                                ? "iOS"
                                : ua.contains("windows")
                                        ? "Windows"
                                        : ua.contains("cros")
                                                ? "ChromeOS"
                                                : ua.contains("mac os") || ua.contains("macintosh")
                                                        ? "macOS"
                                                        : ua.contains("linux")
                                                                ? "Linux"
                                                                : "Unknown";
        String browser =
                ua.contains("edg/") || ua.contains("edga/") || ua.contains("edgios/")
                        ? "Edge"
                        : ua.contains("opr/") || ua.contains("opera")
                                ? "Opera"
                                : ua.contains("samsungbrowser/")
                                        ? "Samsung Internet"
                                        : ua.contains("chrome/") || ua.contains("crios/")
                                                ? "Chrome"
                                                : ua.contains("firefox/") || ua.contains("fxios/")
                                                        ? "Firefox"
                                                        : ua.contains("safari/")
                                                                ? "Safari"
                                                                : "Unknown";
        String device =
                ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")
                        ? "Bot"
                        : ua.contains("ipad")
                                        || ua.contains("tablet")
                                        || (ua.contains("android") && !ua.contains("mobile"))
                                ? "Tablet"
                                : ua.contains("mobile")
                                                || ua.contains("iphone")
                                                || ua.contains("ipod")
                                        ? "Mobile"
                                        : !os.equals("Unknown") ? "Desktop" : "Unknown";
        return new ClickMetadata(referrer(referer), device, os, browser, null, null);
    }

    private String referrer(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() > 2048) return null;
        try {
            URI uri = URI.create(value);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) return null;
            String host = uri.getHost();
            if (host == null) return null;
            host = IDN.toASCII(host).toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            if (host.length() > 253
                    || !host.contains(".")
                    || host.endsWith(".localhost")
                    || host.contains(":")
                    || host.matches("[0-9.]+")) return null;
            return host;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
