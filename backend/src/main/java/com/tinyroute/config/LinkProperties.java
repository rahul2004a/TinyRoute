package com.tinyroute.config;

import com.tinyroute.model.DestinationUrl;
import com.tinyroute.model.ShortCode;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("tinyroute.links")
public class LinkProperties {
    @NotBlank private String shortBaseUrl;
    private String codeKey;
    private String codeSalt;

    public String getShortBaseUrl() {
        return shortBaseUrl;
    }

    public void setShortBaseUrl(String value) {
        new DestinationUrl(value);
        URI uri = URI.create(value);
        if (uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !(uri.getRawPath().isEmpty() || uri.getRawPath().equals("/")))
            throw new IllegalArgumentException("Short base URL must be an HTTPS origin");
        shortBaseUrl = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String shortHost() {
        return DestinationUrl.canonicalHost(URI.create(shortBaseUrl).getHost());
    }

    public String shortUrl(ShortCode code) {
        return shortBaseUrl + "/" + code.value();
    }

    public String getCodeKey() {
        return codeKey;
    }

    public void setCodeKey(String value) {
        codeKey = value;
    }

    public String getCodeSalt() {
        return codeSalt;
    }

    public void setCodeSalt(String value) {
        codeSalt = value;
    }

    public byte[] codeKeyBytes() {
        try {
            if (codeKey == null) throw new IllegalArgumentException();
            byte[] bytes = java.util.Base64.getDecoder().decode(codeKey);
            if (bytes.length != 32
                    || !java.util.Base64.getEncoder().encodeToString(bytes).equals(codeKey))
                throw new IllegalArgumentException();
            return bytes;
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Short code key configuration is invalid");
        }
    }

    public byte[] codeSaltBytes() {
        if (codeSalt == null || !codeSalt.matches("[A-Za-z0-9:_-]{8,64}"))
            throw new IllegalArgumentException("Short code salt configuration is invalid");
        return codeSalt.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
