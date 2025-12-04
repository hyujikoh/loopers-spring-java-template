package com.loopers.application.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {
    private final PaymentService paymentService;
    private final UserService userService;
    private final PgClient pgClient;

    // 여러 도메인 비즈니스 로직 조합 처리
    @CircuitBreaker(name = "pgClient", fallbackMethod = "processPaymentFallback")
    @Retry(name = "pgClient")
    @TimeLimiter(name = "pgClient")
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        UserEntity user = userService.getUserByUsername(command.username());


        // 2. PaymentEntity 생성 및 저장
        PaymentEntity pendingPayment = paymentService.createPending(user, command);

        try{
            PgPaymentResponse pgResponse = pgClient.requestPayment(
                    user.getUsername(),
                    PgPaymentRequest.of(command)
            );
            pendingPayment.updateTransactionKey(pgResponse.transactionKey());

            log.info("PG 결제 요청 완료 - transactionKey: {}, 콜백 대기 중",
                    pgResponse.transactionKey());
            return PaymentInfo.from(pendingPayment);
        }
        catch (Exception e){
            log.error("PG 결제 요청 실패, PENDING 처리", e);
            return PaymentInfo.pending(pendingPayment);
        }

    }

    // Fallback 메서드
    private PaymentInfo processPaymentFallback(PaymentCommand command, Throwable t) {
        log.error("PG 서비스 장애, 결제 요청 실패 처리", t);

        UserEntity userByUsername = userService.getUserByUsername(command.username());
        // PG 호출 자체가 실패한 경우
        PaymentEntity failed = paymentService.createFailedPayment(userByUsername,
                command,
                "결제 시스템이 일시적으로 사용 불가능합니다."
        );

        return PaymentInfo.from(failed);
    }

    public void handlePaymentCallback(PaymentV1Dtos.PgCallbackRequest request) {
        log.info("결제 콜백 처리 시작 - transactionKey: {}", request.transactionKey());

        PaymentEntity payment = paymentService.getByTransactionKey(request.transactionKey());

        // 결제 결과에 따라 상태 업데이트
        switch (request.status()) {
            case "SUCCESS", "COMPLETED", "APPROVED" -> {
                payment.complete();
                log.info("결제 성공 처리 완료 - transactionKey: {}", request.transactionKey());

                // 주문 상태 업데이트 등 후속 처리
                // orderService.confirmPayment(payment.getOrderId());
            }
            case "FAILED", "LIMIT_EXCEEDED", "INVALID_CARD" -> {
                payment.fail(request.reason());
                log.warn("결제 실패 처리 - transactionKey: {}, reason: {}",
                        request.transactionKey(), request.reason());

                // 주문 취소 등 후속 처리
                // orderService.cancelOrder(payment.getOrderId());
            }
            default -> {
                log.error("알 수 없는 결제 상태 - transactionKey: {}, status: {}",
                        request.transactionKey(), request.status());
            }
        }
    }
}
