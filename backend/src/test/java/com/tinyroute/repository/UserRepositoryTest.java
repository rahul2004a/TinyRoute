package com.tinyroute.repository;

import com.tinyroute.model.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID createdUserId;

    @AfterEach
    void removeCreatedUser() {
        if (createdUserId != null) {
            jdbcTemplate.update("delete from users where id = ?", createdUserId);
        }
    }

    @Test
    void savesAndFindsAUserByNormalizedEmail() {
        String email = uniqueEmail();
        User user = userRepository.save(User.create(email));
        createdUserId = user.id();

        assertThat(userRepository.findByEmailNormalized(email))
                .map(User::id)
                .contains(user.id());
    }

    @Test
    void databaseRejectsDuplicateNormalizedEmail() {
        String email = uniqueEmail();
        User firstUser = userRepository.save(User.create(email));
        createdUserId = firstUser.id();

        assertThatThrownBy(() -> userRepository.save(User.create(email)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
