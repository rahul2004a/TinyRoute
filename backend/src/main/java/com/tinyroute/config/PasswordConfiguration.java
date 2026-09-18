package com.tinyroute.config;

import com.tinyroute.security.Argon2PasswordHasher;
import com.tinyroute.security.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PasswordConfiguration {

    @Bean
    PasswordHasher passwordHasher() {
        return new Argon2PasswordHasher();
    }
}
