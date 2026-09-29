package com.tinyroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TinyRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyRouteApplication.class, args);
    }
}
