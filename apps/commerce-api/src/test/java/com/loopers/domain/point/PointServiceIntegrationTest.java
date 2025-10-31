package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;
import com.loopers.fixtures.PointTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.CoreException;
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

    @MockitoSpyBean
    private PointHistoryRepository pointHistoryRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("사용자 등록 시 포인트가 자동으로 생성및 포인트 조회 여부를 확인한다.")
    void get_point_when_user_exists() {
        // given
        UserRegisterCommand command = UserTestFixture.createUserCommand(
                "testuser",
                "dvum0045@gmail.com",
                "1990-01-01",
                Gender.FEMALE);
        UserInfo userInfo = userFacade.registerUser(command);

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

    @Test
    @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
    void charge_with_nonexistent_user_fails() {
        // when & then
        assertThatThrownBy(() -> pointService.charge(
                PointTestFixture.NONEXISTENT_USERNAME,
                PointTestFixture.CHARGE_AMOUNT_1000))
                .isInstanceOf(CoreException.class)
                .hasMessage(PointTestFixture.ERROR_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    void charge_1000_returns_total_amount() {
        // given
        UserRegisterCommand command = UserTestFixture.createUserCommand(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER);
        UserInfo userInfo = userFacade.registerUser(command);

        // when
        java.math.BigDecimal totalAmount = pointService.charge(
                userInfo.username(),
                PointTestFixture.CHARGE_AMOUNT_1000);

        // then
        assertThat(totalAmount).isEqualByComparingTo(PointTestFixture.CHARGE_AMOUNT_1000_SCALED);
    }

    @Test
    @DisplayName("포인트 충전 시, 포인트 내역이 저장된다.")
    void charge_saves_point_history() {
        // given
        UserRegisterCommand command = UserTestFixture.createUserCommand(
                UserTestFixture.DEFAULT_USERNAME,
                UserTestFixture.DEFAULT_EMAIL,
                UserTestFixture.DEFAULT_BIRTHDATE,
                UserTestFixture.DEFAULT_GENDER);
        UserInfo userInfo = userFacade.registerUser(command);

        // when
        pointService.charge(userInfo.username(), PointTestFixture.CHARGE_AMOUNT_1000);

        // then
        verify(pointHistoryRepository).save(any(PointHistoryEntity.class));
    }
}
