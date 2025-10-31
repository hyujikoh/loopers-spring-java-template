package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.user.UserEntity;
import com.loopers.fixtures.PointTestFixture;
import com.loopers.fixtures.UserTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public class PointUnitTest {

    @Test
    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    void charge_with_zero_or_negative_amount_fails() {
        // given
        UserEntity user = UserTestFixture.createDefaultUserEntity();
        PointEntity point = PointTestFixture.createPointEntity(user);

        // when & then
        assertThatThrownBy(() -> point.charge(PointTestFixture.ZERO_AMOUNT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PointTestFixture.ERROR_INVALID_CHARGE_AMOUNT);

        assertThatThrownBy(() -> point.charge(PointTestFixture.NEGATIVE_AMOUNT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PointTestFixture.ERROR_INVALID_CHARGE_AMOUNT);

        assertThatThrownBy(() -> point.charge(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PointTestFixture.ERROR_INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    void charge_1000_returns_total_amount() {
        // given
        UserEntity user = UserTestFixture.createDefaultUserEntity();
        PointEntity point = PointTestFixture.createPointEntity(user);

        // when
        point.charge(PointTestFixture.CHARGE_AMOUNT_1000);

        // then
        PointTestFixture.assertPointAmount(point, PointTestFixture.CHARGE_AMOUNT_1000_SCALED);
    }
}
