package com.tinyroute.service;

import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.PasswordResetRequested;
import com.tinyroute.model.PasswordResetToken;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PasswordResetTokenRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.TokenHashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class PasswordResetRequestProcessor {

    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Clock clock;

    @Autowired
    public PasswordResetRequestProcessor(
            UserRepository userRepository,
            AuthIdentityRepository authIdentityRepository,
            PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this(userRepository, authIdentityRepository, passwordResetTokenRepository, Clock.systemUTC());
    }

    PasswordResetRequestProcessor(
            UserRepository userRepository,
            AuthIdentityRepository authIdentityRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.authIdentityRepository = Objects.requireNonNull(authIdentityRepository);
        this.passwordResetTokenRepository = Objects.requireNonNull(passwordResetTokenRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public boolean prepareDelivery(PasswordResetRequested request) {
        User user = userRepository.findByEmailNormalized(request.email())
                .filter(existing -> existing.deletedAt() == null)
                .orElse(null);
        if (user == null || authIdentityRepository
                .findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD).isEmpty()) {
            return false;
        }

        String tokenHash = TokenHashing.sha256(request.token());
        Instant expiresAt = clock.instant().plus(PASSWORD_RESET_TTL);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserIdForUpdate(user.id())
                .map(existing -> {
                    existing.replace(tokenHash, expiresAt);
                    return existing;
                })
                .orElseGet(() -> PasswordResetToken.create(user, tokenHash, expiresAt));
        passwordResetTokenRepository.save(resetToken);
        return true;
    }
}
