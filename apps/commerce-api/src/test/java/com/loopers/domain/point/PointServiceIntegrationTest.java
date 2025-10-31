package com.loopers.domain.point;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.fixtures.PointTestFixture;
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

    @Test
    @DisplayName("사용자 등록 시 포인트가 자동으로 생성및 포인트 조회 여부를 확인한다.")
    void get_point_when_user_exists() {
        // given
        UserRegisterCommand req = PointTestFixture.AlternativeTestUser.createCommand();
        UserInfo userInfo = userFacade.registerUser(req);

        // when
        PointEntity point = pointService.getByUsername(userInfo.username());

        // then
        PointTestFixture.assertPointEntityValid(point, userInfo.id());
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    void get_point_return_null_when_user_not_exists() {
        // when
        PointEntity point = pointService.getByUsername(PointTestFixture.NONEXISTENT_USERNAME);

        // then
        PointTestFixture.assertPointIsNull(point);
    }

}
