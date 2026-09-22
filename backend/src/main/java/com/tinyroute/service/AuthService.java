package com.tinyroute.service;

import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.exception.AuthenticationFailedException;
import com.tinyroute.exception.OtpExpiredException;
import com.tinyroute.exception.OtpInvalidException;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.AuthenticatedSession;
import com.tinyroute.model.PendingRegistration;
import com.tinyroute.model.RegistrationOtpRequested;
import com.tinyroute.model.RefreshSession;
import com.tinyroute.model.RefreshSessionRotation;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PendingRegistrationRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.JwtTokenService;
import com.tinyroute.security.TokenHashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionStore refreshSessionStore;
    private final JwtRevocationStore jwtRevocationStore;
    private final RegistrationMailCapacity registrationMailCapacity;
    private final String loginFailurePasswordHash;
    private final Clock clock;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            PendingRegistrationRepository pendingRegistrationRepository,
            AuthIdentityRepository authIdentityRepository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            JwtTokenService jwtTokenService,
            RefreshSessionStore refreshSessionStore,
            JwtRevocationStore jwtRevocationStore,
            RegistrationMailCapacity registrationMailCapacity
    ) {
        this(
                userRepository,
                pendingRegistrationRepository,
                authIdentityRepository,
                passwordHasher,
                eventPublisher,
                jwtTokenService,
                refreshSessionStore,
                jwtRevocationStore,
                registrationMailCapacity,
                Clock.systemUTC()
        );
    }

    AuthService(
            UserRepository userRepository,
            PendingRegistrationRepository pendingRegistrationRepository,
            AuthIdentityRepository authIdentityRepository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            JwtTokenService jwtTokenService,
            RefreshSessionStore refreshSessionStore,
            JwtRevocationStore jwtRevocationStore,
            RegistrationMailCapacity registrationMailCapacity,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.pendingRegistrationRepository = Objects.requireNonNull(pendingRegistrationRepository);
        this.authIdentityRepository = Objects.requireNonNull(authIdentityRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService);
        this.refreshSessionStore = Objects.requireNonNull(refreshSessionStore);
        this.jwtRevocationStore = Objects.requireNonNull(jwtRevocationStore);
        this.registrationMailCapacity = Objects.requireNonNull(registrationMailCapacity);
        this.loginFailurePasswordHash = passwordHasher.hash(randomToken());
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public Optional<String> startRegistration(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = clock.instant();
        String pendingToken = randomToken();
        String otp = randomOtp();
        String passwordHash = passwordHasher.hash(password);
        String otpHash = passwordHasher.hash(otp);

        if (userRepository.findByEmailNormalized(normalizedEmail).isPresent()) {
            return Optional.of(pendingToken);
        }
        reserveMailDelivery();

        String pendingTokenHash = TokenHashing.sha256(pendingToken);
        Instant otpExpiresAt = now.plus(OTP_TTL);
        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmailNormalized(normalizedEmail)
                .map(existing -> {
                    existing.replace(pendingTokenHash, passwordHash, otpHash, otpExpiresAt, now);
                    return existing;
                })
                .orElseGet(() -> PendingRegistration.create(
                        pendingTokenHash, normalizedEmail, passwordHash, otpHash, otpExpiresAt, now
                ));
        pendingRegistrationRepository.save(pendingRegistration);
        eventPublisher.publishEvent(new RegistrationOtpRequested(normalizedEmail, otp));
        return Optional.of(pendingToken);
    }

    private void reserveMailDelivery() {
        if (!registrationMailCapacity.tryReserve()) {
            throw new ServiceUnavailableException();
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    registrationMailCapacity.release();
                }
            }
        });
    }

    @Transactional(noRollbackFor = {OtpInvalidException.class, OtpExpiredException.class})
    public AuthenticatedSession verifyRegistration(String pendingToken, String otp) {
        PendingRegistration pendingRegistration = pendingRegistrationRepository
                .findByTokenHashForUpdate(TokenHashing.sha256(pendingToken))
                .orElseThrow(OtpInvalidException::new);
        Instant now = clock.instant();
        if (!now.isBefore(pendingRegistration.otpExpiresAt())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new OtpExpiredException();
        }
        if (!passwordHasher.matches(otp, pendingRegistration.otpHash())) {
            pendingRegistration.recordFailedAttempt();
            if (pendingRegistration.hasExhaustedAttempts()) {
                pendingRegistrationRepository.delete(pendingRegistration);
            }
            throw new OtpInvalidException();
        }

        User user = userRepository.save(User.create(pendingRegistration.emailNormalized()));
        authIdentityRepository.save(AuthIdentity.password(user, pendingRegistration.emailNormalized(), pendingRegistration.passwordHash()));
        pendingRegistrationRepository.delete(pendingRegistration);

        return issueSession(user);
    }

    @Transactional(readOnly = true)
    public AuthenticatedSession login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailNormalized(normalizedEmail).orElse(null);
        if (user == null || user.deletedAt() != null) {
            verifyLoginFailurePassword(password);
            throw new AuthenticationFailedException();
        }
        AuthIdentity passwordIdentity = authIdentityRepository
                .findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD)
                .orElse(null);
        if (passwordIdentity == null) {
            verifyLoginFailurePassword(password);
            throw new AuthenticationFailedException();
        }
        if (!passwordHasher.matches(password, passwordIdentity.secretHash())) {
            throw new AuthenticationFailedException();
        }

        return issueSession(user);
    }

    private void verifyLoginFailurePassword(String password) {
        passwordHasher.matches(password, loginFailurePasswordHash);
    }

    private AuthenticatedSession issueSession(User user) {
        String accessToken = jwtTokenService.issueAccessToken(user.id(), user.tokenVersion());
        String refreshToken = randomToken();
        AccessToken accessTokenClaims = jwtTokenService.verifyAccessToken(accessToken);
        refreshSessionStore.create(
                TokenHashing.sha256(refreshToken), user.id(), user.tokenVersion(), accessTokenClaims.tokenId()
        );
        return new AuthenticatedSession(user.id(), user.emailNormalized(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public String currentSessionEmail(AccessToken accessToken) {
        User user = userRepository.findById(accessToken.userId())
                .filter(existing -> existing.deletedAt() == null)
                .filter(existing -> existing.tokenVersion() == accessToken.tokenVersion())
                .orElseThrow(AuthenticationFailedException::new);
        return user.emailNormalized();
    }

    public String refreshRateLimitSubject(String refreshToken) {
        return refreshSessionStore.findFamilyId(TokenHashing.sha256(refreshToken))
                .orElseThrow(AuthenticationFailedException::new);
    }

    public AuthenticatedSession refresh(String refreshToken) {
        String currentTokenHash = TokenHashing.sha256(refreshToken);
        RefreshSession refreshSession = refreshSessionStore.find(currentTokenHash).orElse(null);
        if (refreshSession == null) {
            refreshSessionStore.rotate(currentTokenHash, TokenHashing.sha256(randomToken()), java.util.UUID.randomUUID());
            throw new AuthenticationFailedException();
        }
        User user = userRepository.findById(refreshSession.userId())
                .filter(existing -> existing.deletedAt() == null)
                .filter(existing -> existing.tokenVersion() == refreshSession.tokenVersion())
                .orElse(null);
        if (user == null) {
            refreshSessionStore.deleteAllByUserId(refreshSession.userId());
            throw new AuthenticationFailedException();
        }

        String replacementAccessToken = jwtTokenService.issueAccessToken(user.id(), user.tokenVersion());
        String replacementRefreshToken = randomToken();
        AccessToken replacementAccessTokenClaims = jwtTokenService.verifyAccessToken(replacementAccessToken);
        RefreshSessionRotation rotation = refreshSessionStore.rotate(
                currentTokenHash,
                TokenHashing.sha256(replacementRefreshToken),
                replacementAccessTokenClaims.tokenId()
        );
        if (rotation.status() != RefreshSessionRotation.Status.ROTATED
                || rotation.session().filter(session -> session.userId().equals(user.id())
                && session.tokenVersion() == user.tokenVersion()).isEmpty()) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticatedSession(user.id(), user.emailNormalized(), replacementAccessToken, replacementRefreshToken);
    }

    public void logout(AccessToken accessToken, String refreshToken) {
        jwtRevocationStore.revoke(accessToken.tokenId(), accessToken.expiresAt());
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshSessionStore.deleteCurrent(
                    TokenHashing.sha256(refreshToken), accessToken.userId(), accessToken.tokenId()
            );
        }
    }

    @Transactional(noRollbackFor = OtpExpiredException.class)
    public void resendRegistrationOtp(String pendingToken) {
        PendingRegistration pendingRegistration = pendingRegistrationRepository
                .findByTokenHashForUpdate(TokenHashing.sha256(pendingToken))
                .orElseThrow(OtpInvalidException::new);
        Instant now = clock.instant();
        if (!now.isBefore(pendingRegistration.otpExpiresAt())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new OtpExpiredException();
        }

        String otp = randomOtp();
        pendingRegistration.replaceOtp(passwordHasher.hash(otp), now.plus(OTP_TTL), now);
        eventPublisher.publishEvent(new RegistrationOtpRequested(pendingRegistration.emailNormalized(), otp));
    }

    private String normalizeEmail(String email) {
        return Normalizer.normalize(Objects.requireNonNull(email).trim(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomOtp() {
        return "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
    }

}
