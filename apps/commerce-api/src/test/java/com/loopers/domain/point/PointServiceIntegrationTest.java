package com.loopers.domain.point;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserDomainCreateRequest;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserFacade userFacade;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("사용자 등록 시 포인트가 자동으로 생성및 포인트 조회 여부를 확인한다.")
    void get_point_when_user_exists() {
        // given
        UserRegisterCommand req =
                new UserRegisterCommand("testuser", "existing@email.com", "1990-01-01", Gender.FEMALE);
        UserInfo userInfo = userFacade.registerUser(req);

        // when
        PointEntity point = pointService.getByUsername(userInfo.username());

        // then
        Assertions.assertThat(point.getAmount()).isNotNull();
        Assertions.assertThat(point.getUser().getId()).isEqualTo(userInfo.id());
        Assertions.assertThat(point.getAmount()).isNotNegative();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    void get_point_return_null_when_user_not_exists() {
        // when
        PointEntity point = pointService.getByUsername("nonexistentuser");

        // then
        Assertions.assertThat(point).isNull();
    }

    private UserDomainCreateRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserDomainCreateRequest(username, email, birthdate, Gender.MALE);
    }

}
