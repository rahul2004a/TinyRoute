package com.tinyroute.client;

import com.tinyroute.config.GoogleOAuthProperties;
import com.tinyroute.exception.OAuthFailedException;
import com.tinyroute.model.GoogleIdentity;
import com.tinyroute.model.OAuthTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringGoogleOidcClientTest {

    private static final OAuthTransaction TRANSACTION = new OAuthTransaction("expected-nonce", "pkce-verifier");

    @Test
    void sendsAnS256ChallengeWithoutExposingTheVerifierAndUsesTheVerifierAtTokenExchange() throws Exception {
        AtomicReference<String> verifierAtExchange = new AtomicReference<>();
        SpringGoogleOAuthClient client = client(validJwt(), request -> {
            verifierAtExchange.set(request.getAuthorizationExchange().getAuthorizationRequest()
                    .getAttribute(PkceParameterNames.CODE_VERIFIER));
            return tokenResponse();
        });

        URI authorizationUri = client.authorizationUri("state", TRANSACTION);
        GoogleIdentity identity = client.exchangeAuthorizationCode("provider-code", TRANSACTION);

        String expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest("pkce-verifier".getBytes(StandardCharsets.US_ASCII))
        );
        assertThat(authorizationUri.getQuery()).contains("code_challenge=" + expectedChallenge);
        assertThat(authorizationUri.getQuery()).doesNotContain("pkce-verifier");
        assertThat(verifierAtExchange.get()).isEqualTo("pkce-verifier");
        assertThat(identity).isEqualTo(new GoogleIdentity("google-subject", "google@example.com"));
    }

    @Test
    void rejectsAnIdTokenWithTheWrongNonceOrWithoutVerifiedEmail() {
        SpringGoogleOAuthClient wrongNonce = client(jwt("other-nonce", true), ignored -> tokenResponse());
        SpringGoogleOAuthClient unverifiedEmail = client(jwt("expected-nonce", false), ignored -> tokenResponse());

        assertThatThrownBy(() -> wrongNonce.exchangeAuthorizationCode("provider-code", TRANSACTION))
                .isInstanceOf(OAuthFailedException.class);
        assertThatThrownBy(() -> unverifiedEmail.exchangeAuthorizationCode("provider-code", TRANSACTION))
                .isInstanceOf(OAuthFailedException.class);
    }

    private SpringGoogleOAuthClient client(
            Jwt jwt,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient
    ) {
        return new SpringGoogleOAuthClient(properties(), tokenClient, ignored -> jwt);
    }

    private OAuth2AccessTokenResponse tokenResponse() {
        return OAuth2AccessTokenResponse.withToken("provider-access-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .additionalParameters(Map.of("id_token", "signed-id-token"))
                .build();
    }

    private Jwt validJwt() {
        return jwt("expected-nonce", true);
    }

    private Jwt jwt(String nonce, boolean emailVerified) {
        Instant now = Instant.now();
        return new Jwt("signed-id-token", now, now.plusSeconds(300), Map.of("alg", "RS256"), Map.of(
                "sub", "google-subject",
                "iss", "https://accounts.google.com",
                "aud", List.of("test-client"),
                "nonce", nonce,
                "email", "google@example.com",
                "email_verified", emailVerified
        ));
    }

    private GoogleOAuthProperties properties() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setRedirectUri("https://api.tinyroute.test/api/auth/google/callback");
        properties.setSuccessUri("https://app.tinyroute.test/links");
        properties.setFailureUri("https://app.tinyroute.test/login?error=oauth_failed");
        return properties;
    }
}
