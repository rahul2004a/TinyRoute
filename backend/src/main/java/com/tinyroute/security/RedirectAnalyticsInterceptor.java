package com.tinyroute.security;

import com.tinyroute.service.*;
import jakarta.servlet.http.*;
import java.net.InetAddress;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RedirectAnalyticsInterceptor implements HandlerInterceptor {
    public static final String SUCCESSFUL_CODE =
            RedirectAnalyticsInterceptor.class.getName() + ".code";
    private final ClickCountService clicks;
    private final ClickMetadataNormalizer normalizer;
    private final ClientAddressResolver addresses;
    private final Clock clock;

    public RedirectAnalyticsInterceptor(
            ClickCountService clicks,
            ClickMetadataNormalizer normalizer,
            ClientAddressResolver addresses,
            Clock clock) {
        this.clicks = clicks;
        this.normalizer = normalizer;
        this.addresses = addresses;
        this.clock = clock;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        if (exception != null
                || !"GET".equals(request.getMethod())
                || response.getStatus() != 302
                || !(request.getAttribute(SUCCESSFUL_CODE) instanceof String code)) return;
        try {
            Instant issuedAt = clock.instant();
            InetAddress address;
            try {
                address = addresses.clientAddress(request);
            } catch (RuntimeException unavailable) {
                address = null;
            }
            clicks.recordSuccessfulRedirect(
                    code,
                    issuedAt,
                    normalizer.normalize(
                            request.getHeader("Referer"),
                            request.getHeader("User-Agent"),
                            request.getHeader("Sec-CH-UA")),
                    address);
        } catch (RuntimeException ignored) {
            /* the redirect is independent of analytics */
        }
    }
}
