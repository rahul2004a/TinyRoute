package com.tinyroute.config;

import com.tinyroute.security.JwtTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties, Clock.systemUTC());
    }
}
