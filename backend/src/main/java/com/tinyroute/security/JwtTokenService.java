package com.tinyroute.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.tinyroute.config.JwtProperties;
import com.tinyroute.model.AccessToken;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class JwtTokenService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final JwtProperties properties;
    private final Clock clock;
    private final JwtEncoder jwtEncoder;
    private final Map<String, JwtDecoder> jwtDecoders;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
        validateProperties();

        RSAPrivateKey signingPrivateKey = decodePrivateKey(properties.getSigningPrivateKeyBase64());
        Map<String, RSAPublicKey> verificationKeys = properties.getVerificationPublicKeys().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> decodePublicKey(entry.getValue())));
        RSAPublicKey signingPublicKey = verificationKeys.get(properties.getActiveKeyId());

        jwtEncoder = NimbusJwtEncoder.withKeyPair(signingPublicKey, signingPrivateKey)
                .algorithm(SignatureAlgorithm.RS256)
                .jwkPostProcessor(builder -> builder.keyID(properties.getActiveKeyId()))
                .build();
        jwtDecoders = verificationKeys.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> decoderFor(entry.getValue())));
    }

    public String issueAccessToken(UUID userId, int tokenVersion) {
        if (userId == null || tokenVersion < 0) {
            throw new IllegalArgumentException("Access token claims are invalid");
        }

        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(java.util.List.of(properties.getAudience()))
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getAccessTokenTtl()))
                .claim("typ", "ACCESS")
                .claim("tokenVersion", tokenVersion)
                .build();
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.getActiveKeyId())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    public AccessToken verifyAccessToken(String token) {
        JwtDecoder decoder = decoderFor(token);
        try {
            Jwt jwt = decoder.decode(token);
            return new AccessToken(
                    UUID.fromString(jwt.getSubject()),
                    UUID.fromString(jwt.getId()),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt(),
                    ((Number) jwt.getClaim("tokenVersion")).intValue()
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private JwtDecoder decoderFor(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new InvalidAccessTokenException();
            }
            JwtDecoder decoder = jwtDecoders.get(signedJwt.getHeader().getKeyID());
            if (decoder == null) {
                throw new InvalidAccessTokenException();
            }
            return decoder;
        } catch (java.text.ParseException | NullPointerException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private JwtDecoder decoderFor(RSAPublicKey publicKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(accessTokenValidator());
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> accessTokenValidator() {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(properties.getClockSkew());
        timestampValidator.setClock(clock);
        timestampValidator.setAllowEmptyExpiryClaim(false);

        return new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, properties.getIssuer()::equals),
                new JwtClaimValidator<Object>(JwtClaimNames.AUD, this::containsExpectedAudience),
                new JwtClaimValidator<Object>(JwtClaimNames.SUB, this::isUuid),
                new JwtClaimValidator<Object>("jti", this::isUuid),
                new JwtClaimValidator<Object>(JwtClaimNames.IAT, Objects::nonNull),
                new JwtClaimValidator<Object>("typ", "ACCESS"::equals),
                new JwtClaimValidator<Object>("tokenVersion", this::isTokenVersion)
        );
    }

    private boolean containsExpectedAudience(Object audience) {
        return audience instanceof Collection<?> values && values.contains(properties.getAudience());
    }

    private boolean isUuid(Object value) {
        if (!(value instanceof String string)) {
            return false;
        }
        try {
            UUID.fromString(string);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isTokenVersion(Object value) {
        return value instanceof Number number
                && number.longValue() >= 0
                && number.longValue() <= Integer.MAX_VALUE;
    }

    private void validateProperties() {
        requireValue(properties.getIssuer());
        requireValue(properties.getAudience());
        requireValue(properties.getActiveKeyId());
        requireValue(properties.getSigningPrivateKeyBase64());
        if (!ACCESS_TOKEN_TTL.equals(properties.getAccessTokenTtl()) || !CLOCK_SKEW.equals(properties.getClockSkew())) {
            throw new IllegalArgumentException("JWT lifetime or clock skew is invalid");
        }
        if (!properties.getVerificationPublicKeys().containsKey(properties.getActiveKeyId())) {
            throw new IllegalArgumentException("JWT active key is unavailable");
        }
    }

    private void requireValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT configuration is incomplete");
        }
    }

    private RSAPrivateKey decodePrivateKey(String encodedKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey))
            );
        } catch (java.security.GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT signing key is invalid");
        }
    }

    private RSAPublicKey decodePublicKey(String encodedKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey))
            );
        } catch (java.security.GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT verification key is invalid");
        }
    }
}
