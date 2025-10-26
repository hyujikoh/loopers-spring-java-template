package com.loopers.domain.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

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
}
