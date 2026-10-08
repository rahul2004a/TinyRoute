package com.tinyroute.config;

import com.tinyroute.cache.ShortCodeCounter;
import com.tinyroute.repository.LinkRepository;
import com.tinyroute.service.InMemoryRedirectRateLimiter;
import com.tinyroute.service.ShortCodeEncoder;
import com.tinyroute.service.ShortCodeGenerator;
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
    ShortCodeEncoder shortCodeEncoder(LinkProperties properties) {
        return new ShortCodeEncoder(properties.codeKeyBytes(), properties.codeSaltBytes());
    }

    @Bean
    ShortCodeGenerator shortCodeGenerator(
            ShortCodeCounter counter, LinkRepository links, ShortCodeEncoder encoder) {
        return new ShortCodeGenerator(counter, links, encoder);
    }
}
