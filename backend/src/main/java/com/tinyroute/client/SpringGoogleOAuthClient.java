package com.tinyroute.client;

import com.tinyroute.config.GoogleOAuthProperties;
import com.tinyroute.model.OAuthTransaction;
import com.tinyroute.model.GoogleIdentity;
import com.tinyroute.exception.OAuthFailedException;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Collection;

@Component
public class SpringGoogleOAuthClient implements GoogleOAuthClient {

    private final GoogleOAuthProperties properties;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient;
    private final JwtDecoder idTokenDecoder;

    @Autowired
    public SpringGoogleOAuthClient(GoogleOAuthProperties properties) {
        this(properties, new RestClientAuthorizationCodeTokenResponseClient(), idTokenDecoder(properties));
    }

    SpringGoogleOAuthClient(
            GoogleOAuthProperties properties,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient,
            JwtDecoder idTokenDecoder
    ) {
        this.properties = properties;
        this.tokenResponseClient = tokenResponseClient;
        this.idTokenDecoder = idTokenDecoder;
    }

    @Override
    public URI authorizationUri(String state, OAuthTransaction transaction) {
        return URI.create(authorizationRequest(state, transaction).getAuthorizationRequestUri());
    }

    @Override
    public GoogleIdentity exchangeAuthorizationCode(String code, OAuthTransaction transaction) {
        try {
            OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(
                            clientRegistration(),
                            new OAuth2AuthorizationExchange(
                                    authorizationRequest("callback-state", transaction),
                                    OAuth2AuthorizationResponse.success(code)
                                            .redirectUri(properties.redirectEndpoint().toString())
                                            .state("callback-state")
                                            .build()
                            )
                    ));
            Object idToken = tokenResponse.getAdditionalParameters().get("id_token");
            if (!(idToken instanceof String rawIdToken) || rawIdToken.isBlank()) {
                throw new OAuthFailedException();
            }
            Jwt jwt = idTokenDecoder.decode(rawIdToken);
            if (!transaction.nonce().equals(jwt.getClaimAsString("nonce"))
                    || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
                throw new OAuthFailedException();
            }
            return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"));
        } catch (OAuthFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OAuthFailedException(exception);
        }
    }

    private OAuth2AuthorizationRequest authorizationRequest(String state, OAuthTransaction transaction) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(properties.authorizationEndpoint().toString())
                .clientId(properties.clientId())
                .redirectUri(properties.redirectEndpoint().toString())
                .scope("openid", "email")
                .state(state)
                .additionalParameters(Map.of(
                        "nonce", transaction.nonce(),
                        "code_challenge", pkceChallenge(transaction.pkceVerifier()),
                        "code_challenge_method", "S256"
                ))
                .attributes(attributes -> attributes.put(PkceParameterNames.CODE_VERIFIER, transaction.pkceVerifier()))
                .build();
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(properties.clientId())
                .clientSecret(properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.redirectEndpoint().toString())
                .scope("openid", "email")
                .authorizationUri(properties.authorizationEndpoint().toString())
                .tokenUri(properties.tokenEndpoint().toString())
                .jwkSetUri(properties.jwkSetEndpoint().toString())
                .issuerUri(properties.issuerEndpoint().toString())
                .clientName("Google")
                .build();
    }

    private static NimbusJwtDecoder idTokenDecoder(GoogleOAuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetEndpoint().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuerEndpoint().toString()),
                new JwtClaimValidator<Object>("aud", audience -> audience instanceof Collection<?> values
                        && values.contains(properties.clientId()))
        ));
        return decoder;
    }

    private String pkceChallenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
