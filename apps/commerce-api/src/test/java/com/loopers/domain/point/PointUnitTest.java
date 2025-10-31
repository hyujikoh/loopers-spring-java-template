package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRegisterRequest;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public class PointUnitTest {

    @Test
    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    void charge_with_zero_or_negative_amount_fails() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("testuser", "test@email.com", "1990-01-01", Gender.MALE);
        UserEntity user = UserEntity.createUserEntity(request);
        PointEntity point = PointEntity.createPointEntity(user);

        // when & then
        assertThatThrownBy(() -> point.charge(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        assertThatThrownBy(() -> point.charge(new BigDecimal("-100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        assertThatThrownBy(() -> point.charge(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    void charge_1000_returns_total_amount() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("testuser", "test@email.com", "1990-01-01", Gender.MALE);
        UserEntity user = UserEntity.createUserEntity(request);
        PointEntity point = PointEntity.createPointEntity(user);

        // when
        point.charge(new BigDecimal("1000"));

        // then
        assertThat(point.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
