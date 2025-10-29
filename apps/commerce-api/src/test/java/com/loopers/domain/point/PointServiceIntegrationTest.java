package com.loopers.domain.point;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("등록된 사용자의 포인트 조회를 성공한다.")
    void get_exist_user_point_amount() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        UserInfo userInfo = userFacade.registerUser(request);

        // when
        PointEntity point = pointService.getByUsername(userInfo.username());

        // then
        Assertions.assertThat(point.getAmount()).isNotNull();
        Assertions.assertThat(point.getUser().getId()).isEqualTo(userInfo.id());
        Assertions.assertThat(point.getAmount()).isNotNegative();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    void no_exist_user_null() {
        // when
        PointEntity point = pointService.getByUsername("nonexistentuser");

        // then
        Assertions.assertThat(point).isNull();
    }

    private UserRegisterRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserRegisterRequest(username, email, birthdate, Gender.MALE);
    }

}
