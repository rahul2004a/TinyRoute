package com.tinyroute.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/** Public candidates only; literal-code/host policy remains in the controller (FR-RED-06). */
@Component
public final class PublicRedirectRequestMatcher implements RequestMatcher {
    @Override
    public boolean matches(HttpServletRequest request) {
        if (request.getDispatcherType() != DispatcherType.REQUEST
                || !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())))
            return false;
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/") || path.length() < 2) return false;
        for (String namespace : new String[] {"api", "actuator", "error"}) {
            if (path.equals("/" + namespace) || path.startsWith("/" + namespace + "/"))
                return false;
        }
        if (path.indexOf('/', 1) >= 0) {
            // Literal malformed nested/trailing paths can get a generic 404. Encoded
            // nested paths stay private so MVC decoding cannot expose an API route.
            return path.matches("/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*/?");
        }
        final String decoded;
        try {
            decoded =
                    org.springframework.web.util.UriUtils.decode(
                            path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (decoded.indexOf('/', 1) >= 0 || decoded.indexOf('\\') >= 0) return false;
        return !java.util.Set.of("/api", "/actuator", "/error").contains(decoded);
    }
}
