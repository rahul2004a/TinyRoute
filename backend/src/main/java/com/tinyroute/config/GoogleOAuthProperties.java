package com.tinyroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("tinyroute.oauth.google")
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";
    private String issuer = "https://accounts.google.com";
    private String redirectUri;
    private String successUri;
    private String failureUri;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAuthorizationUri() { return authorizationUri; }
    public void setAuthorizationUri(String authorizationUri) { this.authorizationUri = authorizationUri; }
    public String getTokenUri() { return tokenUri; }
    public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }
    public String getJwkSetUri() { return jwkSetUri; }
    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getSuccessUri() { return successUri; }
    public void setSuccessUri(String successUri) { this.successUri = successUri; }
    public String getFailureUri() { return failureUri; }
    public void setFailureUri(String failureUri) { this.failureUri = failureUri; }

    public URI authorizationEndpoint() { return httpsUri(authorizationUri, "authorization URI"); }
    public URI redirectEndpoint() { return httpsUri(redirectUri, "redirect URI"); }
    public URI tokenEndpoint() { return httpsUri(tokenUri, "token URI"); }
    public URI jwkSetEndpoint() { return httpsUri(jwkSetUri, "JWK set URI"); }
    public URI issuerEndpoint() { return httpsUri(issuer, "issuer URI"); }
    public URI successEndpoint() { return httpsUri(successUri, "success URI"); }
    public URI failureEndpoint() { return httpsUri(failureUri, "failure URI"); }

    public String clientId() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Google OAuth client ID is required");
        }
        return clientId;
    }

    private URI httpsUri(String value, String description) {
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Google OAuth " + description + " must be an HTTPS URI");
        }
        return uri;
    }
}
