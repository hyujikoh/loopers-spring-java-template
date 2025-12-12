package com.loopers.infrastructure.coupon;

import java.util.Objects;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponConsumeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 도메인 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 도메인 엔티티에서 발행된 쿠폰 이벤트를 처리합니다.
 * Week 7 요구사항: 주문 트랜잭션과 쿠폰 사용 처리를 이벤트로 분리
 *
 * @author hyunjikoh
 * @since 2025. 12. 10.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CouponEventHandler {
    
    private final CouponService couponService;

    /**
     * 쿠폰 사용 이벤트 처리 (핵심 비즈니스 로직)
     * <p>
     * BEFORE_COMMIT으로 주문 트랜잭션과 함께 처리하여 일관성 보장
     * 쿠폰 사용 실패 시 전체 주문 트랜잭션 롤백
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Async
    public void handleCouponConsume(CouponConsumeEvent event) {
        Objects.requireNonNull(event, "쿠폰 이벤트가 null 입니다.");

        CouponEntity coupon = event.coupon();
        log.debug("쿠폰 사용 처리 시작 - orderId={}, userId={}, couponId={}",
                event.orderId(), coupon.getUserId(), coupon.getId());

        couponService.consumeCoupon(coupon);

        log.debug("쿠폰 사용 처리 완료 - orderId={}",
                event.orderId());
    }

    /**
     * 쿠폰 사용 통계 처리 (부가 기능)
     * <p>
     * AFTER_COMMIT으로 주문 완료 후 비동기 처리
     * 실패해도 주문에 영향 없음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsageStatistics(CouponConsumeEvent event) {
        try {
            // 쿠폰 사용 통계, 알림 등 부가 기능
            log.debug("쿠폰 사용 통계 처리 - orderId={}, userId={}, coupons={}",
                    event.orderId(), event.coupon().getUserId(), event.coupon().getId());
            
            // TODO: 쿠폰 사용 통계 업데이트, 마케팅 알림 등 부가 기능 구현
        } catch (Exception e) {
            // 부가 기능 실패는 주문에 영향 없음
            log.error("쿠폰 사용 통계 처리 실패 - orderId={}", event.orderId(), e);
        }
    }
}
