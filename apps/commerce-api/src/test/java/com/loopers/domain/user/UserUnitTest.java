package com.loopers.domain.user;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
class UserUnitTest {
    @DisplayName("User 객체 생성 성공 테스트")
    @Test
    void register_success() {
        String username = "testuser";
        String email = "dvum0045@gmali.com";
        String birthdate = "1990-01-01";
        Gender gender = Gender.MALE;

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate, gender);

        UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

        Assertions.assertThat(userEntity.getUsername()).isEqualTo(username);
        Assertions.assertThat(userEntity.getEmail()).isEqualTo(email);
        Assertions.assertThat(userEntity.getBirthdate()).isEqualTo(LocalDate.parse(birthdate));
        Assertions.assertThat(userEntity.getGender()).isEqualTo(gender);
    }

    @DisplayName("ID가 영문 및 숫자 10자 초과시 생성 실패")
    @Test
    void createUserEntity_fail_when_username_exceeds_10_characters() {
        assertUserCreationFailsWithMessage(
                "testuser123", "test@example.com", "1990-01-01", Gender.MALE,
                "사용자명은 영문 및 숫자 10자 이내여야 합니다."
        );
    }

    @DisplayName("ID가 영문 및 숫자가 아닌 문자 포함시 생성 실패")
    @Test
    void createUserEntity_fail_when_username_contains_invalid_characters() {
        assertUserCreationFailsWithMessage(
                "test@user", "test@example.com", "1990-01-01", Gender.MALE,
                "사용자명은 영문 및 숫자 10자 이내여야 합니다."
        );
    }

    @DisplayName("이메일이 올바른 형식이 아닐 때 생성 실패")
    @Test
    void createUserEntity_fail_when_email_format_invalid() {
        assertUserCreationFailsWithMessage(
                "testuser", "invalid-email", "1990-01-01", Gender.MALE,
                "올바른 형식의 이메일 주소여야 합니다."
        );
    }

    @DisplayName("생년월일이 null일 때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_is_null() {
        assertUserCreationFailsWithMessage(
                "testuser", "test@example.com", null, Gender.MALE,
                "생년월일은 필수 입력값입니다."
        );
    }

    @DisplayName("생년월일이 yyyy-MM-dd 형식이 아닐 때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_format_invalid() {
        assertUserCreationFailsWithMessage(
                "testuser", "test@example.com", "1990/01/01", Gender.MALE,
                "올바른 날짜 형식이어야 합니다."
        );
    }

    @DisplayName("성별이 null일 때 생성 실패")
    @Test
    void createUserEntity_fail_when_gender_is_null() {
        assertUserCreationFailsWithMessage(
                "testuser", "test@example.com", "1990/01/01", null,
                "성별은 필수 입력값입니다."
        );
    }

    /**
     * 사용자 생성 실패 및 메시지 검증 헬퍼 메서드
     *
     * @param username
     * @param email
     * @param birthdate
     * @param expectedMessage
     */
    private void assertUserCreationFailsWithMessage(String username, String email, String birthdate, Gender gender, String expectedMessage) {
        UserRegisterRequest request = new UserRegisterRequest(username, email, birthdate, gender);

        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
