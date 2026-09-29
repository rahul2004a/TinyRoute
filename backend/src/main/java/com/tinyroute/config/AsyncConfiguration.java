package com.tinyroute.config;

import com.tinyroute.client.RegistrationMailCapacity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class AsyncConfiguration {

    @Bean
    TaskExecutor registrationMailExecutor() {
        return boundedMailExecutor("registration-mail-");
    }

    @Bean
    TaskExecutor passwordResetExecutor() {
        return boundedMailExecutor("password-reset-");
    }

    private TaskExecutor boundedMailExecutor(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(RegistrationMailCapacity.MAX_WORKERS);
        executor.setQueueCapacity(RegistrationMailCapacity.QUEUE_CAPACITY);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
