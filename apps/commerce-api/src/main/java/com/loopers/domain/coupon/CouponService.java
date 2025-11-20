package com.loopers.domain.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public CouponEntity createPercentCoupon(UserEntity user, int percent) {
        CouponEntity coupon = CouponEntity.createPercentageCoupon(user, percent);
        return couponRepository.save(coupon);
    }

    @Transactional
    public CouponEntity createFixedAmountCoupon(UserEntity user, BigDecimal fixedAmount) {
        CouponEntity coupon = CouponEntity.createFixedAmountCoupon(user, fixedAmount);
        return couponRepository.save(coupon);
    }


    @Transactional(readOnly = true)
    public CouponEntity getCouponByIdLock(Long couponId) {
        return couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with id: " + couponId));
    }

    @Transactional(readOnly = true)
    public CouponEntity getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with id: " + couponId));
    }

    @Transactional
    public void consumeCoupon(CouponEntity coupon) {
        coupon.use();
    }
}
