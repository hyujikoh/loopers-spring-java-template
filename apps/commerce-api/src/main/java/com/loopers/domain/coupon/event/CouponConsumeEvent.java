package com.loopers.domain.coupon.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author hyunjikoh
 * @since 2025. 12. 9.
 */
public record CouponConsumeEvent (
        List<Long> couponId,
        Long userId,
        Long orderId
){
}
