package com.tinyroute.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tinyroute.config.JwtProperties;
import com.tinyroute.exception.InvalidAccessTokenException;
import com.tinyroute.model.AccessToken;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("a4d548f0-4c0d-45f1-8db3-c43445272b84");

    @Test
    void issuesAndVerifiesAnAccessTokenWithTheRequiredClaims() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService jwtTokenService = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);

        String token = jwtTokenService.issueAccessToken(USER_ID, 3);
        AccessToken accessToken = jwtTokenService.verifyAccessToken(token);

        assertThat(accessToken.userId()).isEqualTo(USER_ID);
        assertThat(accessToken.tokenVersion()).isEqualTo(3);
        assertThat(accessToken.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(accessToken.tokenId()).isNotNull();
    }

    @Test
    void rejectsAnExpiredAccessTokenAfterTheAllowedClockSkew() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService issuer = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);
        String token = issuer.issueAccessToken(USER_ID, 3);
        JwtTokenService verifier = tokenService(
                keyPair,
                "current",
                Map.of("current", publicKey(keyPair)),
                NOW.plus(Duration.ofMinutes(16)).plusSeconds(1)
        );

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenWithAnUnknownKeyIdentifier() throws Exception {
        KeyPair currentKeyPair = rsaKeyPair();
        KeyPair retiredKeyPair = rsaKeyPair();
        JwtTokenService retiredIssuer = tokenService(
                retiredKeyPair,
                "retired",
                Map.of("retired", publicKey(retiredKeyPair)),
                NOW
        );
        String token = retiredIssuer.issueAccessToken(USER_ID, 3);
        JwtTokenService verifier = tokenService(
                currentKeyPair,
                "current",
                Map.of("current", publicKey(currentKeyPair)),
                NOW
        );

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenWithTheWrongIssuer() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService jwtTokenService = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);

        assertThatThrownBy(() -> jwtTokenService.verifyAccessToken(
                signedAccessToken(keyPair, claims("https://another-issuer.test", "tinyroute-web", 3))
        )).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenWithTheWrongAudience() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService jwtTokenService = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);

        assertThatThrownBy(() -> jwtTokenService.verifyAccessToken(
                signedAccessToken(keyPair, claims("https://api.tinyroute.test", "another-audience", 3))
        )).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenSignedWithAnotherAlgorithm() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService jwtTokenService = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("current").build(),
                claims(3)
        );
        token.sign(new MACSigner("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> jwtTokenService.verifyAccessToken(token.serialize()))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenMissingItsTokenVersionClaim() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenService jwtTokenService = tokenService(keyPair, "current", Map.of("current", publicKey(keyPair)), NOW);
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("current").build(),
                claimsWithoutTokenVersion()
        );
        token.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));

        assertThatThrownBy(() -> jwtTokenService.verifyAccessToken(token.serialize()))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    private JwtTokenService tokenService(
            KeyPair signingKeyPair,
            String activeKeyId,
            Map<String, RSAPublicKey> verificationKeys,
            Instant now
    ) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("https://api.tinyroute.test");
        properties.setAudience("tinyroute-web");
        properties.setActiveKeyId(activeKeyId);
        properties.setSigningPrivateKeyBase64(base64(signingKeyPair.getPrivate().getEncoded()));
        properties.setVerificationPublicKeys(
                verificationKeys.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> base64(entry.getValue().getEncoded())))
        );
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setClockSkew(Duration.ofSeconds(60));
        return new JwtTokenService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private JWTClaimsSet claims(int tokenVersion) {
        return claims("https://api.tinyroute.test", "tinyroute-web", tokenVersion);
    }

    private JWTClaimsSet claims(String issuer, String audience, int tokenVersion) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(USER_ID.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))))
                .claim("typ", "ACCESS")
                .claim("tokenVersion", tokenVersion)
                .build();
    }

    private String signedAccessToken(KeyPair keyPair, JWTClaimsSet claims) throws Exception {
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("current").build(),
                claims
        );
        token.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return token.serialize();
    }

    private JWTClaimsSet claimsWithoutTokenVersion() {
        return new JWTClaimsSet.Builder()
                .issuer("https://api.tinyroute.test")
                .audience("tinyroute-web")
                .subject(USER_ID.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))))
                .claim("typ", "ACCESS")
                .build();
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private RSAPublicKey publicKey(KeyPair keyPair) {
        return (RSAPublicKey) keyPair.getPublic();
    }

    private String base64(byte[] encodedKey) {
        return Base64.getEncoder().encodeToString(encodedKey);
    }
}
