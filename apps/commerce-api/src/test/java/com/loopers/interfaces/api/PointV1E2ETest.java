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
import org.springframework.http.*;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.fixtures.PointTestFixture;
import com.loopers.interfaces.api.point.PointV1Dtos;
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
    @DisplayName("X-USER-ID_헤더를_기준으로_사용자의_포인트_조회를_성공한다")
    void get_user_point_by_header_success() {
        // given
        UserRegisterCommand userRegisterCommand = PointTestFixture.TestUser.createCommandOf();
        userFacade.registerUser(userRegisterCommand);

        HttpHeaders headers = PointTestFixture.createUserIdHeaders(PointTestFixture.TestUser.USERNAME);

        // when
        ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType =
                new ParameterizedTypeReference<>() {
                };
        ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, new HttpEntity<>(null, headers),
                        responseType);

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().username()).isEqualTo(PointTestFixture.TestUser.USERNAME),
                () -> assertThat(Objects.requireNonNull(response.getBody()).data().currentPointAmount()).isEqualTo(PointTestFixture.DEFAULT_POINT_AMOUNT)
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

}
