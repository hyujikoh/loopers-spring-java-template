package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponEntity;

/**
 * @author hyunjikoh
 * @since 2025. 12. 9.
 */
public record CouponConsumeEvent(
        CouponEntity coupon,
        Long orderId
) {
}
