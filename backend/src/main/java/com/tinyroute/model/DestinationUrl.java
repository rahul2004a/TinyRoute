package com.tinyroute.model;

import com.tinyroute.exception.LinkValidationException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/** An exact, validated ASCII HTTPS destination; never normalized for Location. */
public record DestinationUrl(String value) {
    public DestinationUrl {
        validate(value);
    }

    public static DestinationUrl parse(String value, String shortHost) {
        URI uri = validate(value);
        if (canonicalHost(uri.getHost()).equals(canonicalHost(shortHost))) {
            throw invalid("Destination must not use TinyRoute's short-link host.");
        }
        return new DestinationUrl(value);
    }

    private static URI validate(String value) {
        if (value == null || value.isEmpty()) throw invalid("Enter a valid HTTPS URL.");
        if (value.length() > 8192) throw invalid("Destination must be 8192 characters or fewer.");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 32 || c >= 127 || c == '\\') throw invalid("Enter a valid HTTPS URL.");
        }
        try {
            URI uri = new URI(value).parseServerAuthority();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.isOpaque()
                    || uri.getRawAuthority() == null) throw invalid("Enter a valid HTTPS URL.");
            if (uri.getRawUserInfo() != null)
                throw invalid("URLs containing credentials are not supported.");
            String authority = uri.getRawAuthority();
            if (authority.endsWith(":")) throw invalid("Enter a valid HTTPS URL.");
            if (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535))
                throw invalid("Enter a valid HTTPS URL.");
            canonicalHost(uri.getHost());
            return uri;
        } catch (URISyntaxException exception) {
            throw invalid("Enter a valid HTTPS URL.");
        }
    }

    /** Canonicalizes only syntactically validated literals; never resolves DNS. */
    public static String canonicalHost(String host) {
        if (host == null || host.isEmpty()) throw invalid("Enter a valid HTTPS URL.");
        String value = host.toLowerCase(Locale.ROOT);
        if (value.startsWith("[") && value.endsWith("]"))
            value = value.substring(1, value.length() - 1);
        if (value.contains(":")) {
            if (!value.matches("[0-9a-f:.]+")) throw invalid("Enter a valid HTTPS URL.");
            if (value.contains(".")) validateIpv4(value.substring(value.lastIndexOf(':') + 1));
            try {
                return InetAddress.getByName(value)
                        .getHostAddress(); // colon/literal grammar prevents DNS
            } catch (UnknownHostException exception) {
                throw invalid("Enter a valid HTTPS URL.");
            }
        }
        if (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        if (value.matches("[0-9.]+")) return validateIpv4(value);
        if (value.length() > 253) throw invalid("Enter a valid HTTPS URL.");
        String[] labels = value.split("\\.", -1);
        String last = labels[labels.length - 1];
        if (last.matches("[0-9]+|0x[0-9a-f]+")) throw invalid("Enter a valid HTTPS URL.");
        for (String label : labels) {
            if (label.length() > 63 || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"))
                throw invalid("Enter a valid HTTPS URL.");
        }
        return value;
    }

    private static String validateIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) throw invalid("Enter a valid HTTPS URL.");
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255)
                throw invalid("Enter a valid HTTPS URL.");
        }
        return value;
    }

    private static LinkValidationException invalid(String message) {
        return new LinkValidationException("destinationUrl", message);
    }
}
