package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import com.loopers.application.user.UserFacade;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.point.PointV1Dtos;
import com.loopers.interfaces.api.user.UserV1Dtos;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1E2ETest {
    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    @Autowired
    private UserFacade userFacade;

    @Autowired
    public PointV1E2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            UserFacade userFacade
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.userFacade = userFacade;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("X-USER-ID 헤더를 기준으로 사용자의 포인트 조회를 성공한다.")
    void get_user_point_success() {

        // given
        String username = "testuser";
        String email = "dvum0045@gmail.com";
        String birthdate = "1990-01-01";
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(username, email, birthdate, Gender.FEMALE);

        userFacade.registerUser(userRegisterRequest);

        // given
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "testuser");

        // act
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

        // assert
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(username),
                () -> {
                    assertThat(Objects.requireNonNull(response.getBody()).data().currentPointAmount()).isEqualTo(BigDecimal.ZERO.setScale(2));
                }
        );
    }


    @Test
    @DisplayName("X-USER-ID 헤더가 없을 경우 400 Bad Request 응답을 반환한다.")
    void getUserPoint_success() {

        // act
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, new HttpEntity<>(null, null), responseType);

        // assert
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );
    }

}
