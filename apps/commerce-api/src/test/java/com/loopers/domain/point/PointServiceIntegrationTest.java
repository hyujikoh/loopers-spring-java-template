package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRegisterRequest;
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
    void get_exist_user_point_amount() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        UserInfo userInfo = userFacade.registerUser(request);

        // when
        PointEntity point = pointService.getByUsername(userInfo.username());

        // then
        assertThat(point.getAmount()).isNotNull();
        assertThat(point.getUser().getId()).isEqualTo(userInfo.id());
        assertThat(point.getAmount()).isNotNegative();
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    void no_exist_user_null() {
        // when
        PointEntity point = pointService.getByUsername("nonexistentuser");

        // then
        assertThat(point).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
    void charge_with_nonexistent_user_fails() {
        // when & then
        assertThatThrownBy(() -> pointService.charge("nonexistentuser", new BigDecimal("1000")))
                .isInstanceOf(CoreException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    void charge_1000_returns_total_amount() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");
        UserInfo userInfo = userFacade.registerUser(request);

        // when
        BigDecimal totalAmount = pointService.charge(userInfo.username(), new BigDecimal("1000"));

        // then
        assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("포인트 충전 시, 포인트 내역이 저장된다.")
    void charge_saves_point_history() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");
        UserInfo userInfo = userFacade.registerUser(request);

        // when
        pointService.charge(userInfo.username(), new BigDecimal("1000"));

        // then
        verify(pointHistoryRepository).save(any(PointHistoryEntity.class));
    }

    private UserRegisterRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserRegisterRequest(username, email, birthdate, Gender.MALE);
    }
}
