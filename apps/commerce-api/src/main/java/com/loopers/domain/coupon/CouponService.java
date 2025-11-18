package com.loopers.domain.coupon;

import org.springframework.stereotype.Component;

import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */

@RequiredArgsConstructor
@Component
public class CouponService {
    private final CouponRepository couponRepository;


    public CouponEntity createPercentCoupon(UserEntity user, int percent) {
        CouponEntity coupon = CouponEntity.createPercentageCoupon(user.getId(), percent);
        return couponRepository.save(coupon);
    }
}
