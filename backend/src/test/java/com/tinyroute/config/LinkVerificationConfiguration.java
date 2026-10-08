package com.tinyroute.config;

import com.tinyroute.controller.LinkVerificationController;
import com.tinyroute.security.PasswordHasher;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration(proxyBeanMethods = false)
public class LinkVerificationConfiguration {
    @Bean
    LinkVerificationController verificationController(
            JdbcTemplate jdbc,
            PasswordHasher hasher,
            LinkProperties properties,
            VerificationTimingCollector timings) {
        return new LinkVerificationController(jdbc, hasher, properties, timings);
    }

    @Bean
    VerificationTimingCollector verificationTimings() {
        return new VerificationTimingCollector();
    }

    @Bean
    @Order(0)
    SecurityFilterChain verificationSecurity(
            HttpSecurity http, @Value("${tinyroute.verification.token}") String token)
            throws Exception {
        if (token.length() < 32)
            throw new IllegalArgumentException("Verification token is required");
        return http.securityMatcher(
                        request ->
                                request.getRequestURI().startsWith("/__verification/")
                                        && request.getRequestURI().length()
                                                > "/__verification/".length())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (request, response, error) ->
                                                        response.setStatus(401))
                                        .accessDeniedHandler(
                                                (request, response, error) ->
                                                        response.setStatus(401)))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .anyRequest()
                                        .access(
                                                (authentication, context) ->
                                                        new AuthorizationDecision(
                                                                allowed(
                                                                        context.getRequest(),
                                                                        token))))
                .build();
    }

    private boolean allowed(HttpServletRequest request, String token) {
        if (!java.util.Set.of("127.0.0.1", "127.0.0.2", "::1", "0:0:0:0:0:0:0:1")
                .contains(request.getRemoteAddr())) return false;
        String supplied = request.getHeader("X-Verification-Token");
        return supplied != null
                && supplied.length() <= 128
                && MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }
}
