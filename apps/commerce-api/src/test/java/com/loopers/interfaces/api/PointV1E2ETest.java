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
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;
import com.loopers.fixtures.UserTestFixture;
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
        UserRegisterCommand userCommand = UserTestFixture.createUserCommand(username, email, birthdate, Gender.FEMALE);

        userFacade.registerUser(userCommand);

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
    @DisplayName("X_USER_ID_헤더가_없을_경우_400_Bad_Request_응답을_반환한다")
    void get_user_point_fail_when_header_missing() {
        // when
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType =
                new ParameterizedTypeReference<>() {
                };
        ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, new HttpEntity<>(null, null), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    void charge_1000_returns_total_amount() {
        // given
        UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
        userFacade.registerUser(userCommand);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userCommand.username());
        headers.setContentType(MediaType.APPLICATION_JSON);

        PointV1Dtos.PointChargeRequest chargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000"));

        // when
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointChargeResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> response =
                testRestTemplate.exchange("/api/v1/points/charge", HttpMethod.POST,
                        new HttpEntity<>(chargeRequest, headers), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(userCommand.username()),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"))
        );
    }

    @Test
    @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
    void charge_with_nonexistent_user_returns_404() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "nonexistentuser");
        headers.setContentType(MediaType.APPLICATION_JSON);

        PointV1Dtos.PointChargeRequest chargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000"));

        // when
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointChargeResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> response =
                testRestTemplate.exchange("/api/v1/points/charge", HttpMethod.POST,
                        new HttpEntity<>(chargeRequest, headers), responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );
    }

}
