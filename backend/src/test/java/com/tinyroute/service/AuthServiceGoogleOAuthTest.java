package com.tinyroute.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.cache.OAuthTransactionStore;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.client.GoogleOAuthClient;
import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.exception.OAuthFailedException;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.model.OAuthTransaction;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PasswordResetTokenRepository;
import com.tinyroute.repository.PendingRegistrationRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.JwtTokenService;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.TokenHashing;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class AuthServiceGoogleOAuthTest {

    private final OAuthTransactionStore transactions = mock(OAuthTransactionStore.class);
    private final GoogleOAuthClient google = mock(GoogleOAuthClient.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthIdentityRepository identities = mock(AuthIdentityRepository.class);
    private final JwtTokenService tokens = mock(JwtTokenService.class);
    private final RefreshSessionStore sessions = mock(RefreshSessionStore.class);

    @ParameterizedTest
    @MethodSource("invalidCallbacks")
    void rejectsInvalidCallbackBeforeProviderExchangeOrSessionIssuance(
            String cookie, String state, String code) {
        when(transactions.consume(anyString()))
                .thenReturn(Optional.of(new OAuthTransaction("nonce", "pkce-verifier")));
        AuthService service = service();

        assertThatThrownBy(() -> service.validateGoogleCallback(cookie, state, code))
                .isInstanceOf(OAuthFailedException.class);

        if (cookie != null && !cookie.isBlank()) {
            verify(transactions).consume(TokenHashing.sha256(cookie));
        } else {
            verifyNoInteractions(transactions);
        }
        verifyNoInteractions(google, users, identities, tokens, sessions);
    }

    static Stream<Arguments> invalidCallbacks() {
        return Stream.of(
                Arguments.of(null, "state", "code"),
                Arguments.of("", "state", "code"),
                Arguments.of(" ", "state", "code"),
                Arguments.of("state", null, "code"),
                Arguments.of("state", "", "code"),
                Arguments.of("state", " ", "code"),
                Arguments.of("state", "attacker-state", "code"),
                Arguments.of("state", "state", null),
                Arguments.of("state", "state", ""),
                Arguments.of("state", "state", " "));
    }

    @Test
    void rejectsAConsumedOrUnknownTransactionBeforeProviderExchange() {
        when(transactions.consume(TokenHashing.sha256("state"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().finishGoogleAuthorization("state", "code"))
                .isInstanceOf(OAuthFailedException.class);

        verifyNoInteractions(google, users, identities, tokens, sessions);
    }

    @Test
    void validatesProviderWithTheConsumedTransactionBeforeIssuingASession() {
        OAuthTransaction transaction = new OAuthTransaction("nonce", "pkce-verifier");
        when(transactions.consume(TokenHashing.sha256("state")))
                .thenReturn(Optional.of(transaction));
        when(google.exchangeAuthorizationCode("code", transaction))
                .thenThrow(new OAuthFailedException());

        assertThatThrownBy(() -> service().finishGoogleAuthorization("state", "code"))
                .isInstanceOf(OAuthFailedException.class);

        verify(google).exchangeAuthorizationCode("code", transaction);
        verifyNoInteractions(users, identities, tokens, sessions);
    }

    @Test
    void rejectsInvalidCallbackAndDiscardsStateBeforeOpeningADatabaseTransaction() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any()))
                .thenThrow(new CannotCreateTransactionException("Database unavailable"));
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.afterPropertiesSet();
        ProxyFactory factory = new ProxyFactory(service());
        factory.addAdvice(interceptor);
        AuthService proxied = (AuthService) factory.getProxy();

        assertThatThrownBy(() -> proxied.validateGoogleCallback("state", "wrong-state", "code"))
                .isInstanceOf(OAuthFailedException.class);

        verify(transactions).consume(TokenHashing.sha256("state"));
        verifyNoInteractions(manager, google, users, identities, tokens, sessions);
    }

    @Test
    void failsUnavailableWhenInvalidCallbackTransactionCleanupFails() {
        when(transactions.consume(TokenHashing.sha256("state")))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> service().validateGoogleCallback("state", "wrong-state", "code"))
                .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(google, users, identities, tokens, sessions);
    }

    private AuthService service() {
        return new AuthService(
                users,
                mock(PendingRegistrationRepository.class),
                identities,
                mock(PasswordHasher.class),
                mock(ApplicationEventPublisher.class),
                tokens,
                sessions,
                mock(JwtRevocationStore.class),
                transactions,
                google,
                mock(RegistrationMailCapacity.class),
                mock(PasswordResetTokenRepository.class),
                mock(LinkService.class),
                mock(AccountDeletionCleanupRepository.class));
    }
}
