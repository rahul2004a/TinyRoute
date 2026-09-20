package com.tinyroute.service;

import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.exception.OtpExpiredException;
import com.tinyroute.exception.OtpInvalidException;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthenticatedSession;
import com.tinyroute.model.PendingRegistration;
import com.tinyroute.model.RegistrationOtpRequested;
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
    private final RegistrationMailCapacity registrationMailCapacity;
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
        this.registrationMailCapacity = Objects.requireNonNull(registrationMailCapacity);
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

        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmailNormalized(normalizedEmail)
                .map(existing -> {
                    existing.replace(TokenHashing.sha256(pendingToken), passwordHash, otpHash, now.plus(OTP_TTL), now);
                    return existing;
                })
                .orElseGet(() -> PendingRegistration.create(
                        TokenHashing.sha256(pendingToken), normalizedEmail, passwordHash, otpHash, now.plus(OTP_TTL), now
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

        String accessToken = jwtTokenService.issueAccessToken(user.id(), user.tokenVersion());
        String refreshToken = randomToken();
        refreshSessionStore.create(TokenHashing.sha256(refreshToken), user.id());
        return new AuthenticatedSession(user.id(), user.emailNormalized(), accessToken, refreshToken);
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
