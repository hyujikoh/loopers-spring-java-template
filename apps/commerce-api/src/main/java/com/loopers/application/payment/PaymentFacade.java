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

import lombok.AllArgsConstructor;
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
    // ✅ Resilience는 여기서 적용!
    @CircuitBreaker(name = "pgClient", fallbackMethod = "processPaymentFallback")
    @Retry(name = "pgClient")
    @TimeLimiter(name = "pgClient")
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        UserEntity user = userService.getUserByUsername(command.username());

        PgPaymentResponse pgResponse = pgClient.requestPayment(
                user.getUsername(),
                PgPaymentRequest.of(command)
        );

        // 2. PaymentEntity 생성 및 저장
        PaymentEntity payment = paymentService.createPayment(user, command, pgResponse);

        return PaymentInfo.from(payment);
    }

    // Fallback 메서드
    private PaymentInfo processPaymentFallback(PaymentCommand command, Throwable t) {
        log.error("PG 결제 실패, PENDING 처리", t);
        PaymentEntity pending = paymentService.createPending(command);
        return PaymentInfo.pending(pending);
    }
}
