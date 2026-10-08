package com.tinyroute.config;

import com.tinyroute.service.ShortCodeGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LinkConfiguration {
    @Bean
    Clock linkClock() {
        return Clock.systemUTC();
    }

    @Bean
    ShortCodeGenerator shortCodeGenerator() {
        return new ShortCodeGenerator(new SecureRandom());
    }
}
