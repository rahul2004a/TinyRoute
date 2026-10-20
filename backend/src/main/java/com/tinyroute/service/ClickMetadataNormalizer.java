package com.tinyroute.service;

import com.tinyroute.model.ClickMetadata;
import java.net.*;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ClickMetadataNormalizer {
    private static final Pattern BRAVE_AGENT =
            Pattern.compile("(?:^|\\s)brave(?:/[0-9.]+)?(?=\\s|$)");
    private static final Pattern CLIENT_HINT_BRAND =
            Pattern.compile(
                    "[ \\t]*\"((?:[\\x20-\\x21\\x23-\\x5b\\x5d-\\x7e]|\\\\[\"\\\\])*+)\""
                            + "[ \\t]*;[ \\t]*v=\"[0-9]{1,4}\"[ \\t]*");

    public ClickMetadata normalize(String referer, String userAgent) {
        return normalize(referer, userAgent, null);
    }

    public ClickMetadata normalize(String referer, String userAgent, String browserBrands) {
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
                BRAVE_AGENT.matcher(ua).find() || hasBraveBrand(browserBrands)
                        ? "Brave"
                        : ua.contains("edg/") || ua.contains("edga/") || ua.contains("edgios/")
                                ? "Edge"
                                : ua.contains("opr/") || ua.contains("opera")
                                        ? "Opera"
                                        : ua.contains("samsungbrowser/")
                                                ? "Samsung Internet"
                                                : ua.contains("chrome/") || ua.contains("crios/")
                                                        ? "Chrome"
                                                        : ua.contains("firefox/")
                                                                        || ua.contains("fxios/")
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

    private boolean hasBraveBrand(String value) {
        if (value == null || value.isBlank() || value.length() > 1024) return false;
        var brands = CLIENT_HINT_BRAND.matcher(value);
        int position = 0;
        boolean brave = false;
        // Consume complete brand/version entries, including quoted GREASE brands. Keep no raw
        // hints.
        while (position < value.length()) {
            if (!brands.region(position, value.length()).lookingAt()) return false;
            brave |= "Brave".equalsIgnoreCase(brands.group(1));
            position = brands.end();
            if (position == value.length()) return brave;
            if (value.charAt(position++) != ',') return false;
        }
        return false;
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
