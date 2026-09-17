package com.tinyroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties("tinyroute.jwt")
public class JwtProperties {

    private String issuer;
    private String audience;
    private String activeKeyId;
    private String signingPrivateKeyBase64;
    private Map<String, String> verificationPublicKeys = new LinkedHashMap<>();
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration clockSkew = Duration.ofSeconds(60);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void setActiveKeyId(String activeKeyId) {
        this.activeKeyId = activeKeyId;
    }

    public String getSigningPrivateKeyBase64() {
        return signingPrivateKeyBase64;
    }

    public void setSigningPrivateKeyBase64(String signingPrivateKeyBase64) {
        this.signingPrivateKeyBase64 = signingPrivateKeyBase64;
    }

    public Map<String, String> getVerificationPublicKeys() {
        return verificationPublicKeys;
    }

    public void setVerificationPublicKeys(Map<String, String> verificationPublicKeys) {
        this.verificationPublicKeys = new LinkedHashMap<>(verificationPublicKeys);
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }
}
