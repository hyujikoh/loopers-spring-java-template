package com.loopers.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.fixtures.UserTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
@DisplayName("UserEntity 단위 테스트")
class UserUnitTest {

    @Nested
    @DisplayName("사용자 생성")
    class CreateUserTest {

        @Test
        @DisplayName("유효한 정보로 User 객체를 생성할 수 있다")
        void create_user_entity_success() {
            // given
            UserDomainCreateRequest userRegisterRequest = UserTestFixture.createDefaultUserDomainRequest();

            // when
            UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

            // then
            UserTestFixture.assertUserEntity(userEntity, userRegisterRequest);
        }
    }

    @Nested
    @DisplayName("사용자 검증")
    class UsernameValidationTest {

        @Test
        @DisplayName("사용자명이 영문 및 숫자 10자를 초과하면 생성에 실패한다")
        void create_user_entity_fail_when_username_exceeds_10_characters() {
            UserTestFixture.assertUserCreationFails(
                    UserTestFixture.InvalidUsername.TOO_LONG,
                    UserTestFixture.DEFAULT_EMAIL,
                    UserTestFixture.DEFAULT_BIRTHDATE,
                    UserTestFixture.DEFAULT_GENDER,
                    UserTestFixture.InvalidUsername.EXPECTED_MESSAGE
            );
        }

        @Test
        @DisplayName("사용자명에 특수문자가 포함되면 생성에 실패한다")
        void create_user_entity_fail_when_username_contains_invalid_characters() {
            UserTestFixture.assertUserCreationFails(
                    UserTestFixture.InvalidUsername.SPECIAL_CHAR,
                    UserTestFixture.DEFAULT_EMAIL,
                    UserTestFixture.DEFAULT_BIRTHDATE,
                    UserTestFixture.DEFAULT_GENDER,
                    UserTestFixture.InvalidUsername.EXPECTED_MESSAGE
            );
        }

        @Test
        @DisplayName("이메일이 올바른 형식이 아니면 생성에 실패한다")
        void create_user_entity_fail_when_email_format_invalid() {
            UserTestFixture.assertUserCreationFails(
                    UserTestFixture.DEFAULT_USERNAME,
                    UserTestFixture.InvalidEmail.INVALID_FORMAT,
                    UserTestFixture.DEFAULT_BIRTHDATE,
                    UserTestFixture.DEFAULT_GENDER,
                    UserTestFixture.InvalidEmail.EXPECTED_MESSAGE
            );
        }

        @Test
        @DisplayName("생년월일이 null이면 생성에 실패한다")
        void create_user_entity_fail_when_birthdate_is_null() {
            UserTestFixture.assertUserCreationFails(
                    UserTestFixture.DEFAULT_USERNAME,
                    UserTestFixture.DEFAULT_EMAIL,
                    null,
                    UserTestFixture.DEFAULT_GENDER,
                    UserTestFixture.InvalidBirthdate.NULL_MESSAGE
            );
        }

        @Test
        @DisplayName("생년월일이 yyyy-MM-dd 형식이 아니면 생성에 실패한다")
        void create_user_entity_fail_when_birthdate_format_invalid() {
            UserTestFixture.assertUserCreationFails(
                    UserTestFixture.DEFAULT_USERNAME,
                    UserTestFixture.DEFAULT_EMAIL,
                    UserTestFixture.InvalidBirthdate.INVALID_FORMAT,
                    UserTestFixture.DEFAULT_GENDER,
                    UserTestFixture.InvalidBirthdate.FORMAT_MESSAGE
            );
        }

        @Test
        @DisplayName("성별이 null이면 생성에 실패한다")
        void create_user_entity_fail_when_gender_is_null() {
            UserTestFixture.assertUserCreationFails(                    UserTestFixture.DEFAULT_USERNAME,
                    UserTestFixture.DEFAULT_EMAIL,
                    UserTestFixture.DEFAULT_BIRTHDATE,
                    null,
                    UserTestFixture.InvalidGender.EXPECTED_MESSAGE
            );
        }
    }



}
