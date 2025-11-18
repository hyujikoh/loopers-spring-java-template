package com.loopers.domain.coupon;

import java.math.BigDecimal;

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
        CouponEntity coupon = CouponEntity.createPercentageCoupon(user, percent);
        return couponRepository.save(coupon);
    }

    public CouponEntity createFixedAmountCoupon(UserEntity user, BigDecimal fixedAmount) {
        CouponEntity coupon = CouponEntity.createFixedAmountCoupon(user, fixedAmount);
        return couponRepository.save(coupon);
    }
}
