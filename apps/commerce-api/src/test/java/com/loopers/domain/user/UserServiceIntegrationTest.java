package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@SpringBootTest
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입시 User 저장이 수행된다.")
    @Test
    void register_success() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");

        // when
        UserEntity result = userService.register(request);

        // then
        assertUserEntity(result, request);
    }

    @DisplayName("이미 가입된 사용자명으로 회원가입 시도 시 실패한다.")
    @Test
    void register_fail_when_username_already_exists() {
        // given
        UserRegisterRequest existingUser = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        userRepository.save(UserEntity.createUserEntity(existingUser));

        UserRegisterRequest duplicateUser = createUserRegisterRequest("testuser", "new@email.com", "1990-01-02");

        // when & then
        assertThatThrownBy(() -> userService.register(duplicateUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 사용자 이름입니다");
    }

    private UserRegisterRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserRegisterRequest(username, email, birthdate);
    }

    private void assertUserEntity(UserEntity actual, UserRegisterRequest expected) {
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getUsername()).isEqualTo(expected.username());
        assertThat(actual.getEmail()).isEqualTo(expected.email());
        assertThat(actual.getBirthdate().toString()).isEqualTo(expected.birthdate());
        assertThat(actual.getCreatedAt()).isNotNull();
        assertThat(actual.getDeletedAt()).isNull();
    }
}
