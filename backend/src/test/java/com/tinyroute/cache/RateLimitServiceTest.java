package com.tinyroute.cache;

import com.tinyroute.config.RateLimitProperties;
import com.tinyroute.model.RateLimitAction;
import com.tinyroute.model.RateLimitCounter;
import com.tinyroute.security.ClientAddressResolver;
import com.tinyroute.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void usesAnHmacOfTheForwardedClientAddressOnlyWhenTheProxyIsTrusted() {
        RecordingRateLimitStore store = new RecordingRateLimitStore();
        RateLimitService service = service(store);

        service.allowClient(RateLimitAction.REGISTER, request("10.12.0.5", "2001:0db8:0:0:0:0:0:1"));
        service.allowClient(RateLimitAction.PASSWORD_LOGIN, request("10.12.0.5", "2001:db8::1"));

        String registerKey = store.keysByAction.get(RateLimitAction.REGISTER);
        String loginKey = store.keysByAction.get(RateLimitAction.PASSWORD_LOGIN);
        assertThat(registerKey).startsWith("rl:auth:register:");
        assertThat(loginKey).startsWith("rl:auth:password-login:");
        assertThat(registerKey.substring(registerKey.lastIndexOf(':') + 1))
                .isEqualTo(loginKey.substring(loginKey.lastIndexOf(':') + 1));
        assertThat(registerKey).doesNotContain("2001:db8");

        service.allowClient(RateLimitAction.REGISTER, request("192.0.2.10", "203.0.113.99"));
        service.allowClient(RateLimitAction.GOOGLE_START, request("192.0.2.10", null));

        String untrustedRegisterKey = store.keysByAction.get(RateLimitAction.REGISTER);
        String directClientKey = store.keysByAction.get(RateLimitAction.GOOGLE_START);
        assertThat(untrustedRegisterKey).doesNotContain("203.0.113.99");
        assertThat(untrustedRegisterKey.substring(untrustedRegisterKey.lastIndexOf(':') + 1))
                .isEqualTo(directClientKey.substring(directClientKey.lastIndexOf(':') + 1));
    }

    @Test
    void appliesAnIndependentConfiguredLimitToEachAuthAction() {
        RecordingRateLimitStore store = new RecordingRateLimitStore();
        RateLimitService service = service(store);
        MockHttpServletRequest request = request("10.12.0.5", "203.0.113.42");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(service.allowClient(RateLimitAction.REGISTER, request).allowed()).isTrue();
        }

        assertThat(service.allowClient(RateLimitAction.REGISTER, request).allowed()).isFalse();
        assertThat(service.allowClient(RateLimitAction.PASSWORD_LOGIN, request).allowed()).isTrue();
    }

    private RateLimitService service(RateLimitStore store) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setHmacSecret("test-rate-limit-secret");
        properties.setTrustedProxyCidrs(java.util.List.of("10.0.0.0/8"));
        return new RateLimitService(store, new ClientAddressResolver(properties));
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    private static final class RecordingRateLimitStore implements RateLimitStore {

        private final Map<String, Long> counts = new HashMap<>();
        private final Map<RateLimitAction, String> keysByAction = new HashMap<>();

        @Override
        public RateLimitCounter increment(String key, Duration window) {
            RateLimitAction action = java.util.Arrays.stream(RateLimitAction.values())
                    .filter(candidate -> key.startsWith("rl:auth:" + candidate.keySegment() + ":"))
                    .findFirst()
                    .orElseThrow();
            keysByAction.put(action, key);
            return new RateLimitCounter(counts.merge(key, 1L, Long::sum), window);
        }
    }
}
