package com.loopers.domain.coupon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import com.loopers.domain.like.LikeEntity;
import com.loopers.domain.user.UserEntity;
import com.loopers.fixtures.UserTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@DisplayName("CouponService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CouponUnitTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Nested
    @DisplayName("쿠폰 생성")
    class CreateCoupon {

        @org.junit.jupiter.api.Test
        @DisplayName("유효한 사용자 ID와 정액으로 정액 쿠폰을 생성하면 성공한다")
        void valid_user_id_and_fixed_amount_creates_fixed_amount_coupon_successfully() {
        	// given
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            // when
            CouponEntity percentageCoupon = CouponEntity.createPercentageCoupon(user.getId(), 30);

            when(couponRepository.save(any(CouponEntity.class))).thenReturn(percentageCoupon);

        	// then
            CouponEntity percentCoupon = couponService.createPercentCoupon(user, 30);
            assertNotNull(percentCoupon);
            assertEquals(user.getId(), percentCoupon.getUserId());
            assertEquals(30, percentCoupon.getPercentage().intValue());
        }

    }

}
