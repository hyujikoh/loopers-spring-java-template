package com.loopers.domain.user;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
class UserEntityTest {
    @DisplayName("User 객체 생성 성공 테스트")
    @Test
    void register_success() {
        String username = "testuser";
        String email = "dvum0045@gmali.com";
        String birthdate = "1990-01-01";

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);

        UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

        Assertions.assertThat(userEntity.getUsername()).isEqualTo(username);
        Assertions.assertThat(userEntity.getEmail()).isEqualTo(email);
        Assertions.assertThat(userEntity.getBirthdate()).isEqualTo(LocalDate.parse(birthdate));
    }


    @DisplayName("ID가 영문 및 숫자 10자 초과시 생성 실패")
    @Test
    void createUserEntity_fail_when_username_exceeds_10_characters() {
        assertUserCreationFails("testuser123121331", "test@example.com", "1990-01-01");
    }

    @DisplayName("ID가 영문 및 숫자가 아닌 문자 포함시 생성 실패")
    @Test
    void createUserEntity_fail_when_username_contains_invalid_characters() {
        assertUserCreationFails("test@user", "test@example.com", "1990-01-01");
    }

    @DisplayName("이메일이 올바른 형식이 아닐 때 생성 실패")
    @Test
    void createUserEntity_fail_when_email_format_invalid() {
        assertUserCreationFails("testuser", "invalid-email", "1990-01-01");
    }

    @DisplayName("생년월일이 null일 때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_is_null() {
        assertUserCreationFails("testuser", "test@example.com", null);
    }

    @DisplayName("생년월일이 yyyy-MM-dd 형식이 아닐 때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_format_invalid() {
        assertUserCreationFails("testuser", "test@example.com", "1990/01/01");
    }

    private void assertUserCreationFails(String username, String email, String birthdate) {
        UserRegisterRequest request = new UserRegisterRequest(username, email, birthdate);

        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
