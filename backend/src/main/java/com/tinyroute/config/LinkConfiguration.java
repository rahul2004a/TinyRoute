package com.tinyroute.config;

import com.tinyroute.service.InMemoryRedirectRateLimiter;
import com.tinyroute.service.ShortCodeGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LinkConfiguration {
    @Bean
    InMemoryRedirectRateLimiter inMemoryRedirectRateLimiter(RateLimitProperties properties) {
        return new InMemoryRedirectRateLimiter(
                properties.getRedirectMaximumAttempts(),
                properties.getRedirectWindow(),
                10_000,
                System::nanoTime);
    }

    @Bean
    Clock linkClock() {
        return Clock.systemUTC();
    }

    @Bean
    ShortCodeGenerator shortCodeGenerator() {
        return new ShortCodeGenerator(new SecureRandom());
    }
}
