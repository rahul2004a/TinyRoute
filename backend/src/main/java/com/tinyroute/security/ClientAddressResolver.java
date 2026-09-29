package com.tinyroute.security;

import com.tinyroute.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;

@Component
public final class ClientAddressResolver {

    private final byte[] hmacSecret;
    private final List<CidrRange> trustedProxies;

    public ClientAddressResolver(RateLimitProperties properties) {
        if (properties.getHmacSecret() == null || properties.getHmacSecret().isBlank()) {
            throw new IllegalArgumentException("Rate limit HMAC secret is required");
        }
        hmacSecret = properties.getHmacSecret().getBytes(StandardCharsets.UTF_8);
        trustedProxies = properties.getTrustedProxyCidrs().stream().map(CidrRange::parse).toList();
    }

    public String clientHash(HttpServletRequest request) {
        InetAddress remoteAddress = parseAddress(request.getRemoteAddr());
        InetAddress clientAddress = isTrusted(remoteAddress)
                ? parseForwardedAddress(request)
                : remoteAddress;
        return hmac(canonicalAddress(clientAddress));
    }

    private boolean isTrusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(range -> range.contains(address));
    }

    private InetAddress parseForwardedAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            throw new IllegalArgumentException("Trusted proxy did not provide a client address");
        }
        String[] addresses = forwardedFor.split(",", -1);
        InetAddress leftmostAddress = null;
        for (int index = addresses.length - 1; index >= 0; index--) {
            InetAddress candidate = parseAddress(addresses[index].trim());
            leftmostAddress = candidate;
            if (!isTrusted(candidate)) {
                return candidate;
            }
        }
        return leftmostAddress;
    }

    private InetAddress parseAddress(String address) {
        if (address == null || !address.matches("[0-9A-Fa-f:.]+") || (!address.contains(":") && !address.contains("."))) {
            throw new IllegalArgumentException("Client address is invalid");
        }
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Client address is invalid", exception);
        }
    }

    private String canonicalAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16 && isIpv4MappedAddress(bytes)) {
            return (bytes[12] & 0xff) + "." + (bytes[13] & 0xff) + "." + (bytes[14] & 0xff) + "." + (bytes[15] & 0xff);
        }
        return address.getHostAddress();
    }

    private boolean isIpv4MappedAddress(byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash client address", exception);
        }
    }

    private record CidrRange(byte[] network, int prefixLength) {

        static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Trusted proxy CIDR is invalid");
            }
            InetAddress network = parseNetworkAddress(parts[0]);
            try {
                int prefixLength = Integer.parseInt(parts[1]);
                if (prefixLength < 0 || prefixLength > network.getAddress().length * Byte.SIZE) {
                    throw new IllegalArgumentException("Trusted proxy CIDR is invalid");
                }
                return new CidrRange(network.getAddress(), prefixLength);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Trusted proxy CIDR is invalid", exception);
            }
        }

        boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            for (int bit = 0; bit < prefixLength; bit++) {
                int byteIndex = bit / Byte.SIZE;
                int bitMask = 1 << (Byte.SIZE - 1 - (bit % Byte.SIZE));
                if ((candidate[byteIndex] & bitMask) != (network[byteIndex] & bitMask)) {
                    return false;
                }
            }
            return true;
        }

        private static InetAddress parseNetworkAddress(String address) {
            if (address == null || !address.matches("[0-9A-Fa-f:.]+") || (!address.contains(":") && !address.contains("."))) {
                throw new IllegalArgumentException("Trusted proxy CIDR is invalid");
            }
            try {
                return InetAddress.getByName(address);
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException("Trusted proxy CIDR is invalid", exception);
            }
        }
    }
}
