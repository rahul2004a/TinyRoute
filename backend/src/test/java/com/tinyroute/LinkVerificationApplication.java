package com.tinyroute;

import com.tinyroute.config.*;
import java.util.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;

/** Test-classpath entry point; never packaged in the production application. */
public final class LinkVerificationApplication {
    private LinkVerificationApplication() {}

    public static void main(String[] args) {
        String token = System.getenv("TINYROUTE_VERIFICATION_TOKEN");
        if (token == null || !token.matches("[A-Za-z0-9_-]{32,128}"))
            throw new IllegalArgumentException(
                    "A random verification token of at least 32 characters is required");
        String rawPort = System.getenv().getOrDefault("TINYROUTE_VERIFICATION_PORT", "8443");
        if (!rawPort.matches("[0-9]{4,5}"))
            throw new IllegalArgumentException("Verification port is invalid");
        int port = Integer.parseInt(rawPort);
        if (port < 1024 || port > 65535)
            throw new IllegalArgumentException("Verification port is invalid");
        new SpringApplicationBuilder(
                        TinyRouteApplication.class,
                        TestJwtTokenConfiguration.class,
                        LinkVerificationConfiguration.class)
                .properties("spring.main.allow-bean-definition-overriding=true")
                .initializers(
                        context -> {
                            if (!Arrays.asList(context.getEnvironment().getActiveProfiles())
                                    .contains("dev"))
                                throw new IllegalStateException(
                                        "Verification requires the externally activated dev profile");
                            Map<String, Object> properties = new HashMap<>();
                            // DynamicPropertyRegistrar is a Spring Test callback, not a normal
                            // SpringApplication callback. Resolve it explicitly before any DB
                            // beans.
                            new TestInfrastructureConfiguration()
                                    .testDatastoreProperties()
                                    .accept(
                                            (name, supplier) ->
                                                    properties.put(name, supplier.get()));
                            properties.put("server.address", "127.0.0.1");
                            properties.put("server.port", port);
                            properties.put("tinyroute.verification.token", token);
                            properties.put(
                                    "tinyroute.links.short-base-url", "https://localhost:" + port);
                            properties.put(
                                    "tinyroute.rate-limit.trusted-proxy-cidrs", "127.0.0.2/32");
                            context.getEnvironment()
                                    .getPropertySources()
                                    .addFirst(
                                            new MapPropertySource(
                                                    "disposable-verification", properties));
                        })
                .run(args);
    }
}
