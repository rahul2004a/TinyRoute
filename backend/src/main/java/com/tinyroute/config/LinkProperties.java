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
}
