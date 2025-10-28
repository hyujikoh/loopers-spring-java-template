package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dtos;
import com.loopers.utils.DatabaseCleanUp;

/**
 * ø
 *
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    public UserV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
    void register_success() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate, Gender.FEMALE);

        // act
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, new HttpEntity<>(userRegisterRequest), responseType);

        // assert
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(username),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().email()).isEqualTo(email),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().birthdate()).isEqualTo(birthdate)
        );
    }

    @Test
    @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
    void register_fail_no_gender() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";
        Gender gender = null;

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate, gender);

        // act
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, new HttpEntity<>(userRegisterRequest), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    @DisplayName("유저 정보 조회 성공 테스트")
    void get_user_info() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";
        Gender gender = Gender.MALE;

        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate, gender);

        userRepository.save(UserEntity.createUserEntity(userRegisterRequest));
        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/users?username=" + username, HttpMethod.GET, null, responseType);


        // then

        // assert
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(username),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().email()).isEqualTo(email),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().birthdate()).isEqualTo(birthdate),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().gender()).isEqualTo(gender)
        );

    }


    @Test
    @DisplayName("존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다.")
    void get_user_info_fail_not_found() {
        // given
        String username = "nonExistentUser";

        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/users?username=" + username, HttpMethod.GET, null, responseType);

        // assert
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );

    }

}
