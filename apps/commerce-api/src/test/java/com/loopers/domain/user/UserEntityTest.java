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
        // Given
        String username = "testuser123121331"; // 11자
        String email = "test@example.com";
        String birthdate = "1990-01-01";

        // When & Then
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);

        // Then
        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(userRegisterRequest)).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @DisplayName("ID가 영문 및 숫자가 아닌 문자 포함시 생성 실패")
    @Test
    void createUserEntity_fail_when_username_contains_invalid_characters() {
        // Given
        String username = "test@user"; // 특수문자 포함
        String email = "test@example.com";
        String birthdate = "1990-01-01";

        // When & Then
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);


        // Then
        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(userRegisterRequest)).isInstanceOf(
                IllegalArgumentException.class
        );

    }

    @DisplayName("이메일이 올바른 형식이 아닐 때 생성 실패")
    @Test
    void createUserEntity_fail_when_email_format_invalid() {
        // Given
        String username = "testuser";
        String email = "invalid-email"; // 잘못된 형식
        String birthdate = "1990-01-01";

        // When & Then
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);


        // Then
        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(userRegisterRequest)).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @DisplayName("생년월일이 null일 때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_is_null() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String birthdate = null;

        // When & Then
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);

        // Then
        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(userRegisterRequest)).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @DisplayName("생년월일이 yyyy-MM-dd 일때 생성 실패")
    @Test
    void createUserEntity_fail_when_birthdate_format_invalid() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String birthdate = "1990/01/01";

        // When & Then
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate);

        // Then
        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(userRegisterRequest)).isInstanceOf(
                IllegalArgumentException.class
        );
    }
}
