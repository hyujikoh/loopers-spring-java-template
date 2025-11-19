package com.loopers.domain.coupon;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface CouponRepository {
    CouponEntity save(CouponEntity any);

    Optional<CouponEntity> findByIdWithLock(Long couponId);
}
