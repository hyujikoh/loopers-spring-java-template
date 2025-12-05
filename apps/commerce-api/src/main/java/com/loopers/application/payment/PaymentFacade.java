package com.loopers.application.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
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
    private final OrderService orderService;  // 주문 정합성 검증을 위해 추가
    private final PgClient pgClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 카드 결제 처리
     *
     * Resilience4j 적용:
     * - Circuit Breaker: PG 장애 시 빠른 실패 (Fallback 실행)
     * - Retry: 일시적 오류 시 재시도 (최대 3회)
     *
     * 타임아웃은 Feign Client 설정으로 처리:
     * - connect-timeout: 5s
     * - read-timeout: 10s
     */
    @CircuitBreaker(name = "pgClient", fallbackMethod = "processPaymentFallback")
    @Retry(name = "pgClient")
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        UserEntity user = userService.getUserByUsername(command.username());

        // 1. 주문 정합성 검증
        validateOrderForPayment(command.orderId(), user.getId(), command.amount());

        // 2. PaymentEntity 생성 및 저장
        PaymentEntity pendingPayment = paymentService.createPending(user, command);

        // 3. PG 결제 요청
        log.info("결제 요청 시작 - orderId: {}, username: {}, amount: {}",
                command.orderId(), command.username(), command.amount());

        PgPaymentResponse pgResponse = pgClient.requestPayment(
                user.getUsername(),
                PgPaymentRequest.of(command)
        );

        // 4. PG API 응답 검증
        if (pgResponse.isApiFail()) {
            // API 호출은 성공했지만, PG에서 FAIL 응답
            String errorMessage = String.format("PG 결제 요청 실패 - errorCode: %s, message: %s",
                    pgResponse.meta().errorCode(),
                    pgResponse.meta().message());
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        // 5. transactionKey 업데이트
        if (pgResponse.data() == null || pgResponse.transactionKey() == null) {
            throw new RuntimeException("PG 응답에 transactionKey가 없습니다.");
        }

        pendingPayment.updateTransactionKey(pgResponse.transactionKey());

        log.info("PG 결제 요청 완료 - orderId: {}, transactionKey: {}, status: {}, 콜백 대기 중",
                command.orderId(), pgResponse.transactionKey(), pgResponse.status());

        return PaymentInfo.from(pendingPayment);

    }

    /**
     * 주문 정합성 검증
     * <p>
     * 결제 요청 시 주문과 동일한 검증 로직:
     * - 주문 존재 여부
     * - 주문 소유자 확인
     * - 주문 상태 확인 (PENDING만 결제 가능)
     * - 결제 금액 일치 여부
     *
     * @param orderId       주문 ID
     * @param userId        사용자 ID
     * @param paymentAmount 결제 금액
     * @throws IllegalArgumentException 정합성 검증 실패 시
     */
    private void validateOrderForPayment(Long orderId, Long userId, java.math.BigDecimal paymentAmount) {
        // 주문 조회
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, userId);

        // 주문 상태 확인 (PENDING 상태만 결제 가능)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    String.format("결제 가능한 주문 상태가 아닙니다. (현재 상태: %s)", order.getStatus())
            );
        }

        // 결제 금액 정합성 검증
        if (order.getFinalTotalAmount().compareTo(paymentAmount) != 0) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 주문 금액과 일치하지 않습니다. (주문 금액: %s, 결제 금액: %s)",
                            order.getFinalTotalAmount(), paymentAmount)
            );
        }

        log.debug("주문 정합성 검증 완료 - orderId: {}, amount: {}", orderId, paymentAmount);
    }

    // Fallback 메서드
    @SuppressWarnings("unused")
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

    /**
     * PG 콜백 처리 (개선)
     *
     * 프로세스:
     * 1. 콜백 수신
     * 2. DB에서 결제 조회
     * 3. 멱등성 체크
     * 4. ⭐ PG에 실제 상태 조회 (보안 강화)
     * 5. ⭐ 데이터 검증 (주문ID, 상태, 금액)
     * 6. ⭐ 주문 금액 vs 결제 금액 검증
     * 7. 상태 업데이트
     * 8. 이벤트 발행
     *
     * 멱등성 보장: PENDING 상태가 아니면 처리하지 않음
     */
    @Transactional
    public void handlePaymentCallback(PaymentV1Dtos.PgCallbackRequest request) {
        log.info("결제 콜백 처리 시작 - transactionKey: {}, status: {}, orderId: {}",
                request.transactionKey(), request.status(), request.orderId());

        // 1. DB에서 결제 조회
        PaymentEntity payment = paymentService.getByTransactionKey(request.transactionKey());

        // 2. 멱등성 체크: 이미 처리된 결제는 무시
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("이미 처리된 결제 - transactionKey: {}, currentStatus: {}",
                    request.transactionKey(), payment.getPaymentStatus());
            return;
        }

        // 3. PG에서 실제 상태 조회
        // userId로 user 조회 (username 필요)
        // Note: PaymentEntity에 username을 저장하거나, UserRepository에 findById 메서드 추가 권장
        // 현재는 orderId로 주문 조회 후 userId로 검증하는 방식 사용
        OrderEntity order = orderService.getOrderByIdAndUserId(payment.getOrderId(), payment.getUserId());
        UserEntity user = userService.getUserById(payment.getUserId());

        PgPaymentResponse pgData = verifyPaymentFromPg(request.transactionKey(), user.getUsername());

        // 4. 콜백 데이터 검증
        validateCallbackData(payment, request, pgData);

        // 5. 주문 금액 검증
        validateOrderAmount(order, pgData.amount());

        // 6. 결제 결과에 따라 상태 업데이트
        switch (request.status()) {
            case "SUCCESS" -> {
                payment.complete();
                log.info("결제 성공 처리 완료 - transactionKey: {}, orderId: {}, amount: {}",
                        request.transactionKey(), request.orderId(), pgData.amount());

                // 이벤트 발행 (OrderFacade 직접 의존 X)
                eventPublisher.publishEvent(new PaymentCompletedEvent(
                        payment.getTransactionKey(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount()
                ));
            }
            case "FAILED" -> {
                payment.fail(request.reason());
                log.warn("결제 실패 처리 - transactionKey: {}, orderId: {}, reason: {}",
                        request.transactionKey(), request.orderId(), request.reason());

                eventPublisher.publishEvent(new PaymentFailedEvent(
                        payment.getTransactionKey(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        request.reason()
                ));
            }
            case "PENDING" -> {
                log.debug("결제 처리 중 상태 콜백 수신 - transactionKey: {}, 처리 대기 중",
                        request.transactionKey());
                // PENDING 상태는 무시 (아직 처리 중)
            }
            default -> {
                log.error("알 수 없는 결제 상태 - transactionKey: {}, status: {}, orderId: {}",
                        request.transactionKey(), request.status(), request.orderId());
            }
        }
    }

    /**
     * PG에서 실제 결제 상태 조회
     *
     * 콜백 데이터를 그대로 신뢰하지 않고,
     * PG에 직접 조회하여 실제 상태를 확인합니다.
     *
     * @param transactionKey 거래 키
     * @param username 사용자명
     * @return PG 결제 응답
     * @throws RuntimeException PG 조회 실패 시
     */
    private PgPaymentResponse verifyPaymentFromPg(String transactionKey, String username) {
        try {
            log.debug("PG 결제 상태 조회 시작 - transactionKey: {}", transactionKey);
            PgPaymentResponse pgData = pgClient.getPayment(username, transactionKey);

            // API 응답 검증
            if (pgData.isApiFail()) {
                String errorMessage = String.format("PG 결제 조회 실패 - errorCode: %s, message: %s",
                        pgData.meta().errorCode(),
                        pgData.meta().message());
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            if (pgData.data() == null) {
                throw new RuntimeException("PG 응답에 결제 데이터가 없습니다.");
            }

            log.debug("PG 결제 상태 조회 완료 - transactionKey: {}, status: {}",
                    transactionKey, pgData.status());
            return pgData;
        } catch (Exception e) {
            log.error("PG 결제 상태 조회 실패 - transactionKey: {}", transactionKey, e);
            throw new RuntimeException("PG 결제 상태 조회에 실패했습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * 콜백 데이터 검증
     *
     * 검증 항목:
     * 1. 주문 ID 일치 (DB vs 콜백)
     * 2. 결제 상태 일치 (PG vs 콜백)
     * 3. 결제 금액 일치 (DB vs PG)
     *
     * @param payment DB에 저장된 결제
     * @param callback 콜백 요청 데이터
     * @param pgData PG에서 조회한 실제 데이터
     * @throws IllegalStateException 검증 실패 시
     */
    private void validateCallbackData(
            PaymentEntity payment,
            PaymentV1Dtos.PgCallbackRequest callback,
            PgPaymentResponse pgData
    ) {
        // 1. 주문 ID 일치 확인
        if (!payment.getOrderId().toString().equals(callback.orderId())) {
            throw new IllegalStateException(
                    String.format("주문 ID 불일치 - DB: %s, Callback: %s",
                            payment.getOrderId(), callback.orderId())
            );
        }

        // 2. PG 상태와 콜백 상태 일치 확인
        if (!pgData.status().equals(callback.status())) {
            throw new IllegalStateException(
                    String.format("결제 상태 불일치 - PG: %s, Callback: %s",
                            pgData.status(), callback.status())
            );
        }

        // 3. 금액 일치 확인 (DB vs PG)
        if (pgData.amount().compareTo(payment.getAmount()) != 0) {
            throw new IllegalStateException(
                    String.format("결제 금액 불일치 - DB: %s, PG: %s",
                            payment.getAmount(), pgData.amount())
            );
        }

        log.debug("콜백 데이터 검증 완료 - transactionKey: {}", callback.transactionKey());
    }

    /**
     * 주문 금액 vs 결제 금액 검증
     *
     * 주문 시 계산된 최종 금액과 실제 결제된 금액이 일치하는지 확인합니다.
     * 금액 변조를 방지하기 위한 필수 검증입니다.
     *
     * @param order 주문 엔티티
     * @param paymentAmount 결제 금액
     * @throws IllegalStateException 금액 불일치 시
     */
    private void validateOrderAmount(OrderEntity order, java.math.BigDecimal paymentAmount) {
        if (order.getFinalTotalAmount().compareTo(paymentAmount) != 0) {
            throw new IllegalStateException(
                    String.format("주문 금액과 결제 금액 불일치 - 주문: %s, 결제: %s",
                            order.getFinalTotalAmount(), paymentAmount)
            );
        }

        log.debug("주문 금액 검증 완료 - orderId: {}, amount: {}", order.getId(), paymentAmount);
    }
}
