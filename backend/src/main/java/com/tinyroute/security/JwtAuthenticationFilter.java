package com.tinyroute.security;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.exception.InvalidAccessTokenException;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final JwtRevocationStore jwtRevocationStore;
    private final UserRepository userRepository;
    private final SecurityErrorResponseWriter securityErrors;
    private final PublicRedirectRequestMatcher publicRedirects;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            JwtRevocationStore jwtRevocationStore,
            UserRepository userRepository,
            SecurityErrorResponseWriter securityErrors,
            PublicRedirectRequestMatcher publicRedirects) {
        this.jwtTokenService = jwtTokenService;
        this.jwtRevocationStore = jwtRevocationStore;
        this.userRepository = userRepository;
        this.securityErrors = securityErrors;
        this.publicRedirects = publicRedirects;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicRedirects.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> accessCookie = accessCookie(request);
        if (accessCookie.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessToken accessToken = jwtTokenService.verifyAccessToken(accessCookie.get());
            if (jwtRevocationStore.isRevoked(accessToken.tokenId())) {
                filterChain.doFilter(request, response);
                return;
            }
            Optional<User> user = userRepository.findById(accessToken.userId());
            if (user.isEmpty()
                    || user.get().deletedAt() != null
                    || user.get().tokenVersion() != accessToken.tokenVersion()) {
                filterChain.doFilter(request, response);
                return;
            }
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    accessToken, null, List.of()));
        } catch (InvalidAccessTokenException exception) {
            SecurityContextHolder.clearContext();
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            securityErrors.sessionUnavailable(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> accessCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            String value = cookie.getValue();
            if (AuthCookieService.ACCESS_COOKIE_NAME.equals(cookie.getName())
                    && value != null
                    && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
