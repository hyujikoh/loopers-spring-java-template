package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
@ExtendWith(MockitoExtension.class)
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

    // 회원 가입시 User 저장이 수행된다. ( spy 검증 )
    // 이미 가입된 ID 로 회원가입 시도 시, 실패한다.

    @DisplayName("회원 가입시 User 저장이 수행된다.")
    @org.junit.jupiter.api.Test
    void register_try_success() {
    	// given
        String username = "testuser";
        String email = "dvum0045@gmali.com";
        String birthdate = "1990-01-01";

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);

        // when
        UserEntity registerUser = userService.register(userRegisterRequest);

    	// then
        Assertions.assertThat(registerUser.getId()).isNotNull();
        Assertions.assertThat(registerUser.getUsername()).isEqualTo(username);
        Assertions.assertThat(registerUser.getEmail()).isEqualTo(email);
        Assertions.assertThat(registerUser.getBirthdate().toString()).isEqualTo(birthdate);
        Assertions.assertThat(registerUser.getCreatedAt()).isNotNull();
        Assertions.assertThat(registerUser.getDeletedAt()).isNull();
    }

    @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
    @org.junit.jupiter.api.Test
    void register_2_try_fail() {
        // given

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest("testuser", "dvum0045@gmali.com", "1990-01-01");

        UserRegisterRequest user2RegisterRequest = new UserRegisterRequest("testuser", "dvum0046@gmali.com", "1990-01-03");

        // when
        userService.register(userRegisterRequest);

        Assertions.assertThatThrownBy(() -> userService.register(user2RegisterRequest))
                .isInstanceOf(IllegalArgumentException.class);


    }
}
