package com.loopers.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.fixtures.UserTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
class UserEntityTest {

    @DisplayName("User 객체 생성 성공 테스트")
    @Test
    void create_user_entity_success() {
        // given
        UserDomainCreateRequest userRegisterRequest = UserTestFixture.createUserDomainRequest(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER
        );

        // when
        UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

        // then
        UserTestFixture.assertUserEntity(userEntity, userRegisterRequest);
    }

    @DisplayName("ID가 영문 및 숫자 10자 초과시 생성 실패")
    @Test
    void create_user_entity_fail_when_username_exceeds_10_characters() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.InvalidUsername.TOO_LONG,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER,
                UserTestFixture.InvalidUsername.EXPECTED_MESSAGE
        );
    }

    @DisplayName("ID가 영문 및 숫자가 아닌 문자 포함시 생성 실패")
    @Test
    void create_user_entity_fail_when_username_contains_invalid_characters() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.InvalidUsername.SPECIAL_CHAR,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER,
                UserTestFixture.InvalidUsername.EXPECTED_MESSAGE
        );
    }

    @DisplayName("이메일이 올바른 형식이 아닐 때 생성 실패")
    @Test
    void create_user_entity_fail_when_email_format_invalid() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.InvalidEmail.INVALID_FORMAT,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER,
                UserTestFixture.InvalidEmail.EXPECTED_MESSAGE
        );
    }

    @DisplayName("생년월일이 null일 때 생성 실패")
    @Test
    void create_user_entity_fail_when_birthdate_is_null() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                null,
                UserTestFixture.DEFAULT_GENDER,
                UserTestFixture.InvalidBirthdate.NULL_MESSAGE
        );
    }

    @DisplayName("생년월일이 yyyy-MM-dd 형식이 아닐 때 생성 실패")
    @Test
    void create_user_entity_fail_when_birthdate_format_invalid() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.InvalidBirthdate.INVALID_FORMAT,
                UserTestFixture.DEFAULT_GENDER,
                UserTestFixture.InvalidBirthdate.FORMAT_MESSAGE
        );
    }

    @DisplayName("성별이 null일 때 생성 실패")
    @Test
    void create_user_entity_fail_when_gender_is_null() {
        UserTestFixture.assertUserCreationFails(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                null,
                UserTestFixture.InvalidGender.EXPECTED_MESSAGE
        );
    }
}
