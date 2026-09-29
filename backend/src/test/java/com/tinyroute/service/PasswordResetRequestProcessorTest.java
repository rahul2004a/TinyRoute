package com.tinyroute.service;

import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.PasswordResetRequested;
import com.tinyroute.model.PasswordResetToken;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PasswordResetTokenRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.TokenHashing;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetRequestProcessorTest {

    private static final Instant NOW = Instant.parse("2026-09-23T00:00:00Z");

    @Test
    void replacesOlderTokenWithAHashOnlyForAnEligiblePasswordIdentity() {
        User user = user("user@example.com");
        PasswordResetToken existingToken = PasswordResetToken.create(
                user, TokenHashing.sha256("older-token"), NOW.plusSeconds(60)
        );
        UserRepository users = mock(UserRepository.class);
        AuthIdentityRepository identities = mock(AuthIdentityRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        when(users.findByEmailNormalized("user@example.com")).thenReturn(Optional.of(user));
        when(identities.findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD))
                .thenReturn(Optional.of(AuthIdentity.password(user, user.emailNormalized(), "existing-hash")));
        when(resetTokens.findByUserIdForUpdate(user.id())).thenReturn(Optional.of(existingToken));

        boolean eligible = processor(users, identities, resetTokens)
                .prepareDelivery(new PasswordResetRequested("user@example.com", "new-token"));

        assertThat(eligible).isTrue();
        assertThat(existingToken.tokenHash()).isEqualTo(TokenHashing.sha256("new-token"));
        assertThat(existingToken.expiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));
        verify(resetTokens).save(existingToken);
    }

    @Test
    void skipsPersistenceAndDeliveryForAnUnknownAccount() {
        UserRepository users = mock(UserRepository.class);
        AuthIdentityRepository identities = mock(AuthIdentityRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        when(users.findByEmailNormalized("unknown@example.com")).thenReturn(Optional.empty());

        boolean eligible = processor(users, identities, resetTokens)
                .prepareDelivery(new PasswordResetRequested("unknown@example.com", "new-token"));

        assertThat(eligible).isFalse();
        verify(resetTokens, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private PasswordResetRequestProcessor processor(
            UserRepository users,
            AuthIdentityRepository identities,
            PasswordResetTokenRepository resetTokens
    ) {
        return new PasswordResetRequestProcessor(
                users,
                identities,
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
