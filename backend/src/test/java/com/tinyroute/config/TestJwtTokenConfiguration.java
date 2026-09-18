package com.tinyroute.config;

import com.tinyroute.security.JwtTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class TestJwtTokenConfiguration {

    @Bean
    JwtTokenService jwtTokenService() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("https://api.tinyroute.test");
        properties.setAudience("tinyroute-web");
        properties.setActiveKeyId("test");
        properties.setSigningPrivateKeyBase64(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        properties.setVerificationPublicKeys(Map.of(
                "test",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        ));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setClockSkew(Duration.ofSeconds(60));
        return new JwtTokenService(properties, Clock.systemUTC());
    }
}
