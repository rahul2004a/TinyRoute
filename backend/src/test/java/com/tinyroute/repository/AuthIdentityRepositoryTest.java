package com.tinyroute.repository;

import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthProvider;
import com.tinyroute.model.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class AuthIdentityRepositoryTest {

    @Autowired
    private AuthIdentityRepository authIdentityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private String createdGoogleSubject;

    @AfterEach
    void removeCreatedRecords() {
        if (createdGoogleSubject != null) {
            jdbcTemplate.update(
                    "delete from auth_identities where provider = ? and subject = ?",
                    AuthProvider.GOOGLE.name(),
                    createdGoogleSubject
            );
        }
        for (UUID userId : createdUserIds) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
    }

    @Test
    void findsAnIdentityByProviderAndSubject() {
        User user = saveUser();
        String subject = uniqueGoogleSubject();
        AuthIdentity identity = authIdentityRepository.save(AuthIdentity.google(user, subject));
        createdGoogleSubject = subject;

        assertThat(authIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, subject))
                .map(AuthIdentity::id)
                .contains(identity.id());
    }

    @Test
    void databaseRejectsConcurrentGoogleProviderSubjectDuplicates() throws Exception {
        User firstUser = saveUser();
        User secondUser = saveUser();
        String subject = uniqueGoogleSubject();
        createdGoogleSubject = subject;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<UUID> firstAttempt = executor.submit(saveGoogleIdentity(firstUser, subject, ready, start));
            Future<UUID> secondAttempt = executor.submit(saveGoogleIdentity(secondUser, subject, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> failures = new ArrayList<>();
            int successes = completedAttempts(firstAttempt, secondAttempt, failures);

            assertThat(successes).isEqualTo(1);
            assertThat(failures).singleElement().isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    private User saveUser() {
        User user = userRepository.save(User.create("user-" + UUID.randomUUID() + "@example.com"));
        createdUserIds.add(user.id());
        return user;
    }

    private String uniqueGoogleSubject() {
        return "google-subject-" + UUID.randomUUID();
    }

    private Callable<UUID> saveGoogleIdentity(
            User user,
            String subject,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent identity insert did not start");
            }
            return authIdentityRepository.save(AuthIdentity.google(user, subject)).id();
        };
    }

    private int completedAttempts(Future<UUID> firstAttempt, Future<UUID> secondAttempt, List<Throwable> failures)
            throws InterruptedException {
        int successes = 0;
        for (Future<UUID> attempt : List.of(firstAttempt, secondAttempt)) {
            try {
                attempt.get(5, TimeUnit.SECONDS);
                successes++;
            } catch (ExecutionException exception) {
                failures.add(exception.getCause());
            } catch (java.util.concurrent.TimeoutException exception) {
                failures.add(exception);
            }
        }
        return successes;
    }
}
