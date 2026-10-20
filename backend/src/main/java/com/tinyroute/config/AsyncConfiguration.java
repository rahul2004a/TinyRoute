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

    @Bean
    ThreadPoolTaskExecutor analyticsExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(512);
        executor.setThreadNamePrefix("analytics-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
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
