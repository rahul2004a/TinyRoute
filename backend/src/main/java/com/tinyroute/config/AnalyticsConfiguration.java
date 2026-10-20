package com.tinyroute.config;

import com.tinyroute.security.RedirectAnalyticsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration(proxyBeanMethods = false)
public class AnalyticsConfiguration implements WebMvcConfigurer {
    private final RedirectAnalyticsInterceptor interceptor;

    public AnalyticsConfiguration(RedirectAnalyticsInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
