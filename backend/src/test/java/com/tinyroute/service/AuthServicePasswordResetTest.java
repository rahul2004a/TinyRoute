package com.tinyroute.service;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.cache.OAuthTransactionStore;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.client.GoogleOAuthClient;
import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.PasswordResetRequested;
import com.tinyroute.model.PasswordResetToken;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PasswordResetTokenRepository;
import com.tinyroute.repository.PendingRegistrationRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.JwtTokenService;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.TokenHashing;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServicePasswordResetTest {

    private static final Instant NOW = Instant.parse("2026-09-23T00:00:00Z");

    @Test
    void replacesOlderTokenWithAHashAndPublishesOnlyTheFragmentTokenForAnEligiblePasswordIdentity() {
        User user = user("user@example.com");
        PasswordResetToken existingToken = PasswordResetToken.create(
                user, TokenHashing.sha256("older-token"), NOW.plusSeconds(60)
        );
        UserRepository users = mock(UserRepository.class);
        AuthIdentityRepository identities = mock(AuthIdentityRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        when(users.findByEmailNormalized("user@example.com")).thenReturn(Optional.of(user));
        when(identities.findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD))
                .thenReturn(Optional.of(AuthIdentity.password(user, user.emailNormalized(), "existing-hash")));
        when(resetTokens.findByUserIdForUpdate(user.id())).thenReturn(Optional.of(existingToken));

        service(users, identities, resetTokens, events).requestPasswordReset(" USER@example.com ");

        org.mockito.ArgumentCaptor<PasswordResetRequested> event = org.mockito.ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events).publishEvent(event.capture());
        assertThat(existingToken.tokenHash()).isEqualTo(TokenHashing.sha256(event.getValue().token()));
        assertThat(existingToken.tokenHash()).isNotEqualTo(event.getValue().token());
        assertThat(existingToken.expiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));
        verify(resetTokens).save(existingToken);
    }

    @Test
    void atomicallyChangesThePasswordInvalidatesSessionsAndConsumesTheResetToken() {
        User user = user("user@example.com");
        AuthIdentity identity = AuthIdentity.password(user, user.emailNormalized(), "old-hash");
        PasswordResetToken resetToken = PasswordResetToken.create(
                user, TokenHashing.sha256("valid-token"), NOW.plusSeconds(30 * 60)
        );
        UserRepository users = mock(UserRepository.class);
        AuthIdentityRepository identities = mock(AuthIdentityRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        RefreshSessionStore refreshSessions = mock(RefreshSessionStore.class);
        when(resetTokens.findByTokenHashForUpdate(TokenHashing.sha256("valid-token"))).thenReturn(Optional.of(resetToken));
        when(identities.findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD)).thenReturn(Optional.of(identity));

        AuthService service = service(users, identities, resetTokens, events, refreshSessions);
        service.confirmPasswordReset("valid-token", "replacement-password-12");

        assertThat(identity.secretHash()).isEqualTo("hashed-replacement-password-12");
        assertThat(user.tokenVersion()).isEqualTo(1);
        verify(resetTokens).delete(resetToken);
        verify(refreshSessions).deleteAllByUserId(user.id());
    }

    private AuthService service(
            UserRepository users,
            AuthIdentityRepository identities,
            PasswordResetTokenRepository resetTokens,
            ApplicationEventPublisher events
    ) {
        return service(users, identities, resetTokens, events, mock(RefreshSessionStore.class));
    }

    private AuthService service(
            UserRepository users,
            AuthIdentityRepository identities,
            PasswordResetTokenRepository resetTokens,
            ApplicationEventPublisher events,
            RefreshSessionStore refreshSessions
    ) {
        PasswordHasher passwords = mock(PasswordHasher.class);
        when(passwords.hash(anyString())).thenAnswer(invocation -> "hashed-" + invocation.getArgument(0));
        return new AuthService(
                users,
                mock(PendingRegistrationRepository.class),
                identities,
                passwords,
                events,
                mock(JwtTokenService.class),
                refreshSessions,
                mock(JwtRevocationStore.class),
                mock(OAuthTransactionStore.class),
                mock(GoogleOAuthClient.class),
                mock(RegistrationMailCapacity.class),
                resetTokens,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private User user(String email) {
        User user = User.create(email);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
