package com.tinyroute.service;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.cache.OAuthTransactionStore;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.client.GoogleOAuthClient;
import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.exception.AuthenticationFailedException;
import com.tinyroute.exception.OAuthFailedException;
import com.tinyroute.exception.OtpInvalidException;
import com.tinyroute.exception.RefreshConcurrentException;
import com.tinyroute.exception.ResetTokenInvalidException;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.AuthenticatedSession;
import com.tinyroute.model.GoogleIdentity;
import com.tinyroute.model.OAuthAuthorization;
import com.tinyroute.model.OAuthTransaction;
import com.tinyroute.model.PasswordResetRequested;
import com.tinyroute.model.PasswordResetToken;
import com.tinyroute.model.PendingRegistration;
import com.tinyroute.model.RefreshSession;
import com.tinyroute.model.RefreshSessionRotation;
import com.tinyroute.model.RegistrationOtpRequested;
import com.tinyroute.model.User;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.PasswordResetTokenRepository;
import com.tinyroute.repository.PendingRegistrationRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.JwtTokenService;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.TokenHashing;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final OAuthTransactionStore oauthTransactionStore;
    private final GoogleOAuthClient googleOAuthClient;
    private final RegistrationMailCapacity registrationMailCapacity;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LinkService linkService;
    private final AccountDeletionCleanupRepository accountDeletionCleanupRepository;
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
            OAuthTransactionStore oauthTransactionStore,
            GoogleOAuthClient googleOAuthClient,
            RegistrationMailCapacity registrationMailCapacity,
            PasswordResetTokenRepository passwordResetTokenRepository,
            LinkService linkService,
            AccountDeletionCleanupRepository accountDeletionCleanupRepository) {
        this(
                userRepository,
                pendingRegistrationRepository,
                authIdentityRepository,
                passwordHasher,
                eventPublisher,
                jwtTokenService,
                refreshSessionStore,
                jwtRevocationStore,
                oauthTransactionStore,
                googleOAuthClient,
                registrationMailCapacity,
                passwordResetTokenRepository,
                linkService,
                accountDeletionCleanupRepository,
                Clock.systemUTC());
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
            OAuthTransactionStore oauthTransactionStore,
            GoogleOAuthClient googleOAuthClient,
            RegistrationMailCapacity registrationMailCapacity,
            PasswordResetTokenRepository passwordResetTokenRepository,
            LinkService linkService,
            AccountDeletionCleanupRepository accountDeletionCleanupRepository,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.pendingRegistrationRepository = Objects.requireNonNull(pendingRegistrationRepository);
        this.authIdentityRepository = Objects.requireNonNull(authIdentityRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService);
        this.refreshSessionStore = Objects.requireNonNull(refreshSessionStore);
        this.jwtRevocationStore = Objects.requireNonNull(jwtRevocationStore);
        this.oauthTransactionStore = Objects.requireNonNull(oauthTransactionStore);
        this.googleOAuthClient = Objects.requireNonNull(googleOAuthClient);
        this.registrationMailCapacity = Objects.requireNonNull(registrationMailCapacity);
        this.passwordResetTokenRepository = Objects.requireNonNull(passwordResetTokenRepository);
        this.linkService = Objects.requireNonNull(linkService);
        this.accountDeletionCleanupRepository =
                Objects.requireNonNull(accountDeletionCleanupRepository);
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

        reserveMailDelivery();
        if (userRepository.findByEmailNormalized(normalizedEmail).isPresent()) {
            registrationMailCapacity.release();
            return Optional.of(pendingToken);
        }

        String pendingTokenHash = TokenHashing.sha256(pendingToken);
        Instant otpExpiresAt = now.plus(OTP_TTL);
        PendingRegistration pendingRegistration =
                pendingRegistrationRepository
                        .findByEmailNormalized(normalizedEmail)
                        .map(
                                existing -> {
                                    existing.replace(
                                            pendingTokenHash,
                                            passwordHash,
                                            otpHash,
                                            otpExpiresAt,
                                            now);
                                    return existing;
                                })
                        .orElseGet(
                                () ->
                                        PendingRegistration.create(
                                                pendingTokenHash,
                                                normalizedEmail,
                                                passwordHash,
                                                otpHash,
                                                otpExpiresAt,
                                                now));
        pendingRegistrationRepository.save(pendingRegistration);
        eventPublisher.publishEvent(new RegistrationOtpRequested(normalizedEmail, otp));
        return Optional.of(pendingToken);
    }

    private void reserveMailDelivery() {
        if (!registrationMailCapacity.tryReserve()) {
            throw new ServiceUnavailableException();
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            registrationMailCapacity.release();
                        }
                    }
                });
    }

    @Transactional(noRollbackFor = OtpInvalidException.class)
    public AuthenticatedSession verifyRegistration(String pendingToken, String otp) {
        PendingRegistration pendingRegistration =
                pendingRegistrationRepository
                        .findByTokenHashForUpdate(TokenHashing.sha256(pendingToken))
                        .orElse(null);
        if (pendingRegistration == null) {
            passwordHasher.matches(otp, loginFailurePasswordHash);
            throw new OtpInvalidException();
        }
        Instant now = clock.instant();
        if (!now.isBefore(pendingRegistration.otpExpiresAt())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            passwordHasher.matches(otp, loginFailurePasswordHash);
            throw new OtpInvalidException();
        }
        if (!passwordHasher.matches(otp, pendingRegistration.otpHash())) {
            pendingRegistration.recordFailedAttempt();
            if (pendingRegistration.hasExhaustedAttempts()) {
                pendingRegistrationRepository.delete(pendingRegistration);
            }
            throw new OtpInvalidException();
        }

        String email = pendingRegistration.emailNormalized();
        acquireEmailCreationLock(email);
        if (userRepository.findByEmailNormalized(email).isPresent()) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new OtpInvalidException();
        }
        User user = userRepository.save(User.create(pendingRegistration.emailNormalized()));
        authIdentityRepository.save(
                AuthIdentity.password(
                        user,
                        pendingRegistration.emailNormalized(),
                        pendingRegistration.passwordHash()));
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
        AuthIdentity passwordIdentity =
                authIdentityRepository
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
                TokenHashing.sha256(refreshToken),
                user.id(),
                user.tokenVersion(),
                accessTokenClaims.tokenId());
        return new AuthenticatedSession(
                user.id(), user.emailNormalized(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public String currentSessionEmail(AccessToken accessToken) {
        User user =
                userRepository
                        .findById(accessToken.userId())
                        .filter(existing -> existing.deletedAt() == null)
                        .filter(existing -> existing.tokenVersion() == accessToken.tokenVersion())
                        .orElseThrow(AuthenticationFailedException::new);
        return user.emailNormalized();
    }

    public String refreshRateLimitSubject(String refreshToken) {
        return refreshSessionStore
                .findFamilyId(TokenHashing.sha256(refreshToken))
                .orElseThrow(AuthenticationFailedException::new);
    }

    public AuthenticatedSession refresh(String refreshToken) {
        String currentTokenHash = TokenHashing.sha256(refreshToken);
        RefreshSession refreshSession = refreshSessionStore.find(currentTokenHash).orElse(null);
        if (refreshSession == null) {
            RefreshSessionRotation rotation =
                    refreshSessionStore.rotate(
                            currentTokenHash,
                            TokenHashing.sha256(randomToken()),
                            java.util.UUID.randomUUID());
            if (rotation.status() == RefreshSessionRotation.Status.CONCURRENT) {
                throw new RefreshConcurrentException();
            }
            throw new AuthenticationFailedException();
        }
        User user =
                userRepository
                        .findById(refreshSession.userId())
                        .filter(existing -> existing.deletedAt() == null)
                        .filter(
                                existing ->
                                        existing.tokenVersion() == refreshSession.tokenVersion())
                        .orElse(null);
        if (user == null) {
            refreshSessionStore.deleteAllByUserId(refreshSession.userId());
            throw new AuthenticationFailedException();
        }

        String replacementAccessToken =
                jwtTokenService.issueAccessToken(user.id(), user.tokenVersion());
        String replacementRefreshToken = randomToken();
        AccessToken replacementAccessTokenClaims =
                jwtTokenService.verifyAccessToken(replacementAccessToken);
        RefreshSessionRotation rotation =
                refreshSessionStore.rotate(
                        currentTokenHash,
                        TokenHashing.sha256(replacementRefreshToken),
                        replacementAccessTokenClaims.tokenId());
        if (rotation.status() == RefreshSessionRotation.Status.CONCURRENT) {
            throw new RefreshConcurrentException();
        }
        if (rotation.status() != RefreshSessionRotation.Status.ROTATED
                || rotation.session()
                        .filter(
                                session ->
                                        session.userId().equals(user.id())
                                                && session.tokenVersion() == user.tokenVersion())
                        .isEmpty()) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticatedSession(
                user.id(), user.emailNormalized(), replacementAccessToken, replacementRefreshToken);
    }

    public void logout(AccessToken accessToken, String refreshToken) {
        jwtRevocationStore.revoke(accessToken.tokenId(), accessToken.expiresAt());
        String tokenHash =
                refreshToken == null || refreshToken.isBlank()
                        ? null
                        : TokenHashing.sha256(refreshToken);
        if (!refreshSessionStore.deleteCurrent(
                tokenHash, accessToken.userId(), accessToken.tokenId())) {
            throw new AuthenticationFailedException();
        }
    }

    @Transactional
    public void deleteAccount(AccessToken accessToken) {
        User user =
                userRepository
                        .findByIdForUpdate(accessToken.userId())
                        .filter(existing -> existing.deletedAt() == null)
                        .filter(existing -> existing.tokenVersion() == accessToken.tokenVersion())
                        .orElseThrow(AuthenticationFailedException::new);
        String oldEmail = user.emailNormalized();
        Instant now = clock.instant();

        linkService.tombstoneOwnedLinks(user.id());
        passwordResetTokenRepository.deleteByUser_Id(user.id());
        pendingRegistrationRepository.deleteByEmailNormalized(oldEmail);
        authIdentityRepository.deleteAllByUser_Id(user.id());
        user.deleteAndAnonymize(now);
        accountDeletionCleanupRepository.save(AccountDeletionCleanup.session(user.id(), now));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        String token = randomToken();
        eventPublisher.publishEvent(new PasswordResetRequested(normalizedEmail, token));
    }

    @Transactional(noRollbackFor = ResetTokenInvalidException.class)
    public void confirmPasswordReset(String token, String newPassword) {
        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHashForUpdate(TokenHashing.sha256(token))
                        .orElseThrow(ResetTokenInvalidException::new);
        if (!clock.instant().isBefore(resetToken.expiresAt())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new ResetTokenInvalidException();
        }

        User user = resetToken.user();
        AuthIdentity passwordIdentity =
                authIdentityRepository
                        .findByUserIdAndProvider(user.id(), AuthProvider.PASSWORD)
                        .orElseThrow(ResetTokenInvalidException::new);
        passwordIdentity.replacePasswordHash(passwordHasher.hash(newPassword));
        user.incrementTokenVersion();
        passwordResetTokenRepository.delete(resetToken);
        refreshSessionStore.deleteAllByUserId(user.id());
    }

    public OAuthAuthorization startGoogleAuthorization() {
        String state = randomToken();
        OAuthTransaction transaction = new OAuthTransaction(randomToken(), randomToken());
        try {
            oauthTransactionStore.create(TokenHashing.sha256(state), transaction);
            return new OAuthAuthorization(
                    state, googleOAuthClient.authorizationUri(state, transaction));
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    private void discardGoogleAuthorization(String state) {
        if (state == null || state.isBlank()) {
            return;
        }
        try {
            oauthTransactionStore.consume(TokenHashing.sha256(state));
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    public void validateGoogleCallback(String stateCookie, String state, String code) {
        if (!hasMatchingState(stateCookie, state) || code == null || code.isBlank()) {
            discardGoogleAuthorization(stateCookie);
            throw new OAuthFailedException();
        }
    }

    @Transactional
    public AuthenticatedSession finishGoogleAuthorization(String state, String code) {
        OAuthTransaction transaction;
        try {
            transaction =
                    oauthTransactionStore
                            .consume(TokenHashing.sha256(state))
                            .orElseThrow(OAuthFailedException::new);
        } catch (OAuthFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException(exception);
        }
        GoogleIdentity googleIdentity =
                googleOAuthClient.exchangeAuthorizationCode(code, transaction);
        String email = normalizeEmail(googleIdentity.email());
        acquireEmailCreationLock(email);
        userRepository.acquireAccountCreationLock("google-subject:" + googleIdentity.subject());
        AuthIdentity existingIdentity =
                authIdentityRepository
                        .findByProviderAndSubject(AuthProvider.GOOGLE, googleIdentity.subject())
                        .orElse(null);
        if (existingIdentity != null) {
            if (existingIdentity.user().deletedAt() != null) {
                throw new OAuthFailedException();
            }
            return issueSession(existingIdentity.user());
        }

        if (userRepository.findByEmailNormalized(email).isPresent()) {
            throw new OAuthFailedException();
        }
        User user = userRepository.save(User.create(email));
        authIdentityRepository.save(AuthIdentity.google(user, googleIdentity.subject()));
        return issueSession(user);
    }

    private boolean hasMatchingState(String stateCookie, String state) {
        if (stateCookie == null || state == null || stateCookie.isBlank() || state.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                stateCookie.getBytes(StandardCharsets.US_ASCII),
                state.getBytes(StandardCharsets.US_ASCII));
    }

    private void acquireEmailCreationLock(String email) {
        userRepository.acquireAccountCreationLock("email:" + email);
    }

    @Transactional
    public void resendRegistrationOtp(String pendingToken) {
        PendingRegistration pendingRegistration =
                pendingRegistrationRepository
                        .findByTokenHashForUpdate(TokenHashing.sha256(pendingToken))
                        .orElse(null);
        Instant now = clock.instant();
        if (pendingRegistration == null) {
            passwordHasher.hash(randomOtp());
            return;
        }
        if (!now.isBefore(pendingRegistration.otpExpiresAt())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            passwordHasher.hash(randomOtp());
            return;
        }

        String otp = randomOtp();
        pendingRegistration.replaceOtp(passwordHasher.hash(otp), now.plus(OTP_TTL), now);
        eventPublisher.publishEvent(
                new RegistrationOtpRequested(pendingRegistration.emailNormalized(), otp));
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
