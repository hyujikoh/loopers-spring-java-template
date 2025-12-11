package com.loopers.application.coupon;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponConsumeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 9.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CouponOrderEventHandler {
    private final CouponService couponService;



    //TODO : 결국 트랜잭션은 처리를 하는게 맞지만 동기로 처리하는게 맞지 않을까? 동기와 비동기를 선택하는 기준이 별도 이벤트로 나눴지만 그래도 같은 스레드에 올려는 놔야하지 않을까?
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCouponConsume(CouponConsumeEvent event) {
        // 쿠폰 사용 처리 (실패 시 전체 롤백)
        List<CouponEntity> coupons = couponService.getCouponByIdsAndUserId(
                event.couponId(), event.userId());

        for (CouponEntity coupon : coupons) {
            if (coupon.isUsed()) {
                throw new IllegalStateException("이미 사용된 쿠폰입니다: " + coupon.getId());
            }
            couponService.consumeCoupon(coupon);
        }
    }

    // 2. 부가 기능 - 비동기 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsageStatistics(CouponConsumeEvent event) {
        // 쿠폰 사용 통계, 알림 등 부가 기능
        // 실패해도 주문에 영향 없음
    }

}
