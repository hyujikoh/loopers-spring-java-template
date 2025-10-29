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
import com.loopers.domain.user.UserDomainCreateRequest;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
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
    private final DatabaseCleanUp databaseCleanUp;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    public UserV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("올바른_API_요청으로_회원가입이_성공하면_생성된_사용자_정보를_응답한다")
    void 올바른_API_요청으로_회원가입이_성공하면_생성된_사용자_정보를_응답한다() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";

        UserV1Dtos.UserRegisterRequest apiRequest = new UserV1Dtos.UserRegisterRequest(username, email, birthdate, Gender.FEMALE);

        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, new HttpEntity<>(apiRequest), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(username),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().email()).isEqualTo(email)
        );
    }

    @Test
    @DisplayName("성별이_null인_회원가입_요청시_400_Bad_Request_응답을_반환한다")
    void 성별이_null인_회원가입_요청시_400_Bad_Request_응답을_반환한다() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";
        Gender gender = null;

        UserV1Dtos.UserRegisterRequest apiRequest = new UserV1Dtos.UserRegisterRequest(username, email, birthdate, gender);

        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, new HttpEntity<>(apiRequest), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    @DisplayName("등록된_사용자명으로_사용자_조회시_사용자_정보를_응답한다")
    void 등록된_사용자명으로_사용자_조회시_사용자_정보를_응답한다() {
        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";
        Gender gender = Gender.MALE;

        UserEntity userEntity = UserEntity.createUserEntity(
                new UserDomainCreateRequest(username, email, birthdate, gender)
        );
        userRepository.save(userEntity);

        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/users?username=" + username, HttpMethod.GET, null, responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(username),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().email()).isEqualTo(email),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().gender()).isEqualTo(gender)
        );
    }

    @Test
    @DisplayName("존재하지_않는_사용자명으로_조회시_404_Not_Found_응답을_반환한다")
    void 존재하지_않는_사용자명으로_조회시_404_Not_Found_응답을_반환한다() {
        // given
        String username = "nonExistentUser";

        // when
        ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/users?username=" + username, HttpMethod.GET, null, responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );

    }

}
