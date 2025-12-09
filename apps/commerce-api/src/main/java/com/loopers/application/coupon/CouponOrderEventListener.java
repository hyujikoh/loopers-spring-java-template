package com.loopers.application.coupon;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponConsumeEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 9.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CouponOrderEventListener {
    private final CouponService couponService;
    private final OrderFacade orderFacade;



    //TODO : 결국 트랜잭션은 처리를 하는게 맞지만 동기로 처리하는게 맞지 않을까? 동기와 비동기를 선택하는 기준이 별도 이벤트로 나눴지만 그래도 같은 스레드에 올려는 놔야하지 않을까?
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(CouponConsumeEvent event) {
        log.info("쿠폰 사용 이벤트 처리 시작 - 쿠폰 ID: {}", event.couponId());

        try {
            List<CouponEntity> couponByIdsAndUserId = couponService.getCouponByIdsAndUserId(
                    event.couponId(),
                    event.userId()
            );

            if(!couponByIdsAndUserId.isEmpty()) {
                for (CouponEntity coupon : couponByIdsAndUserId) {
                    couponService.consumeCoupon(coupon);
                }
            }

            log.info("쿠폰 사용 완료 - 쿠폰 ID: {}", event.couponId());
        } catch (Exception e) {
            log.error("쿠폰 사용 실패 - 쿠폰 ID: {}", event.couponId(), e);
            // 보상 트랜잭션 필요 (주문 취소 등)
            //orderFacade.cancelOrderByPaymentFailure(event.orderId(), event.userId());
        }
    }

}
