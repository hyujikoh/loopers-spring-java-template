package com.loopers.application.payment;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.PaymentTimeoutEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 타임아웃 처리 스케줄러
 * <p>
 * PENDING 상태로 오래 대기 중인 결제 건을 TIMEOUT 처리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 5분마다 PENDING 상태로 10분 이상 대기 중인 결제 건을 TIMEOUT 처리
     */
    @Scheduled(fixedDelay = 300000) // 5분 (300,000ms)
    @Transactional
    public void handleTimeoutPayments() {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(10);

        List<PaymentEntity> timeoutPayments =
                paymentService.findPendingPaymentsOlderThan(timeoutThreshold);

        if (timeoutPayments.isEmpty()) {
            return;
        }

        log.warn("결제 타임아웃 처리 시작 - 대상 건수: {}", timeoutPayments.size());

        for (PaymentEntity payment : timeoutPayments) {
            try {
                log.warn("결제 타임아웃 처리 - transactionKey: {}, orderId: {}, requestedAt: {}",
                        payment.getTransactionKey(),
                        payment.getOrderId(),
                        payment.getRequestedAt());

                payment.timeout();

                // 이벤트 발행 (OrderFacade 직접 의존 X)
                eventPublisher.publishEvent(new PaymentTimeoutEvent(
                        payment.getTransactionKey(),
                        payment.getOrderId(),
                        payment.getUserId()
                ));
            } catch (Exception e) {
                log.error("결제 타임아웃 처리 실패 - transactionKey: {}, orderId: {}",
                        payment.getTransactionKey(), payment.getOrderId(), e);
            }
        }

        log.info("결제 타임아웃 처리 완료 - 처리 건수: {}", timeoutPayments.size());
    }
}

