package com.loopers.infrastructure.dataplatform;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.order.event.OrderDataPlatformEvent;
import com.loopers.domain.payment.event.PaymentDataPlatformEvent;
import com.loopers.infrastructure.dataplatform.dto.OrderDataDto;
import com.loopers.infrastructure.dataplatform.dto.PaymentDataDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 플랫폼 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 주문/결제 완료 후 데이터 플랫폼으로 데이터를 전송합니다.
 * Week 7 요구사항: 트랜잭션 분리를 통한 외부 시스템 연동
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 주문 데이터 플랫폼 전송 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 주문 트랜잭션과 완전 분리
     * 데이터 플랫폼 전송 실패가 주문 처리에 영향 주지 않음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDataPlatform(OrderDataPlatformEvent event) {
        try {
            log.debug("주문 데이터 플랫폼 전송 시작 - orderId: {}, eventType: {}",
                    event.orderId(), event.eventType());

            OrderDataDto orderData = OrderDataDto.from(event);
            boolean success = dataPlatformClient.sendOrderData(orderData);

            if (success) {
                log.info("주문 데이터 플랫폼 전송 성공 - orderId: {}, eventType: {}",
                        event.orderId(), event.eventType());
            } else {
                log.warn("주문 데이터 플랫폼 전송 실패 - orderId: {}, eventType: {}",
                        event.orderId(), event.eventType());
                // TODO: 실패한 이벤트를 재처리 큐에 넣거나 알림 발송 등 보상 로직 추가 가능
            }

        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패해도 주문 처리에는 영향 없음
            log.error("주문 데이터 플랫폼 전송 중 예외 발생 - orderId: {}, eventType: {}",
                    event.orderId(), event.eventType(), e);

            // TODO: 실패한 이벤트를 재처리 큐에 넣거나 알림 발송 등 보상 로직 추가 가능
        }
    }

    /**
     * 결제 데이터 플랫폼 전송 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 결제 트랜잭션과 완전 분리
     * 데이터 플랫폼 전송 실패가 결제 처리에 영향 주지 않음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentDataPlatform(PaymentDataPlatformEvent event) {
        try {
            log.debug("결제 데이터 플랫폼 전송 시작 - transactionKey: {}, eventType: {}",
                    event.transactionKey(), event.eventType());

            PaymentDataDto paymentData = PaymentDataDto.from(event);
            boolean success = dataPlatformClient.sendPaymentData(paymentData);

            if (success) {
                log.info("결제 데이터 플랫폼 전송 성공 - transactionKey: {}, eventType: {}",
                        event.transactionKey(), event.eventType());
            } else {
                log.warn("결제 데이터 플랫폼 전송 실패 - transactionKey: {}, eventType: {}",
                        event.transactionKey(), event.eventType());
                // TODO: 실패한 이벤트를 재처리 큐에 넣거나 알림 발송 등 보상 로직 추가 가능
            }

        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패해도 결제 처리에는 영향 없음
            log.error("결제 데이터 플랫폼 전송 중 예외 발생 - transactionKey: {}, eventType: {}",
                    event.transactionKey(), event.eventType(), e);

            // TODO: 실패한 이벤트를 재처리 큐에 넣거나 알림 발송 등 보상 로직 추가 가능
        }
    }
}
