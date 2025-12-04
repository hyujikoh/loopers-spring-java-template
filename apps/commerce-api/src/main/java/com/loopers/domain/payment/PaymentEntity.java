package com.loopers.domain.payment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_transaction_key", columnList = "transactionKey"),
        @Index(name = "idx_payment_order_id", columnList = "orderId")
})
public class PaymentEntity extends BaseEntity {

    @Column(nullable = true, unique = true, length = 50)
    private String transactionKey;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false, length = 50)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String cardType;

    @Column(nullable = false, length = 20)
    private String cardNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, length = 255)
    private String callbackUrl;

    // TODO : TransactionStatus 로 변경 필요
    @Column(nullable = true, length = 20)
    private String transactionStatus;

    @Column(length = 20)
    private String failureReason;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    // → PG에 실제 결제 요청을 보낸 시각
    @Column(name = "requested_at", nullable = false)
    private ZonedDateTime requestedAt;

    /**
     * 결제 생성 팩토리 메서드
     *
     * @param request
     * @return
     */
    public static PaymentEntity createPayment(PaymentDomainCreateRequest request) {
        Objects.requireNonNull(request, "결제 생성 요청은 null일 수 없습니다.");
        return new PaymentEntity(request);
    }


    private PaymentEntity(PaymentDomainCreateRequest request) {
        Objects.requireNonNull(request, "결제 생성 요청은 필수입니다.");
        Objects.requireNonNull(request.transactionKey(), "거래 키는 필수입니다.");
        Objects.requireNonNull(request.orderId(), "주문 ID는 필수입니다.");
        Objects.requireNonNull(request.amount(), "결제 금액은 필수입니다.");
        Objects.requireNonNull(request.cardType(), "카드 타입은 필수입니다.");
        Objects.requireNonNull(request.cardNo(), "카드 번호는 필수입니다.");
        Objects.requireNonNull(request.paymentStatus(), "결제 상태는 필수입니다.");
        Objects.requireNonNull(request.callbackUrl(), "콜백 URL은 필수입니다.");
        Objects.requireNonNull(request.requestedAt(), "요청 시각은 필수입니다.");

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }

        this.transactionKey = request.transactionKey();
        this.orderId = request.orderId();
        this.amount = request.amount();
        this.cardType = request.cardType();
        this.cardNo = request.cardNo();
        this.paymentStatus = request.paymentStatus();
        this.callbackUrl = request.callbackUrl();
        this.userId = request.userId();
        this.requestedAt = request.requestedAt();
    }

    public static PaymentEntity createPending(UserEntity user, PaymentCommand command) {
        PaymentDomainCreateRequest request = new PaymentDomainCreateRequest(
                user.getId(),
                command.orderId(),
                null,
                command.cardType(),
                command.cardNo(),
                command.callbackUrl(),
                command.amount(),
                PaymentStatus.PENDING,
                ZonedDateTime.now(),
                null
        );

        return createPayment(request);

    }

    public static PaymentEntity createFailed(UserEntity user, PaymentCommand command, String reason) {
        PaymentDomainCreateRequest request = new PaymentDomainCreateRequest(
                user.getId(),
                command.orderId(),
                null,
                command.cardType(),
                command.cardNo(),
                command.callbackUrl(),
                command.amount(),
                PaymentStatus.FAILED,
                ZonedDateTime.now(),
                reason
        );
        return createPayment(request);
    }

    public void updateTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    /**
     * 결제 완료 처리
     */
    public void complete() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                String.format("PENDING 상태의 결제만 완료 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.completedAt = ZonedDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String reason) {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                String.format("PENDING 상태의 결제만 실패 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.failureReason = reason;
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /**
     * 결제 타임아웃 처리
     */
    public void timeout() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                String.format("PENDING 상태의 결제만 타임아웃 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.failureReason = "결제 콜백 타임아웃 (10분 초과)";
        this.paymentStatus = PaymentStatus.TIMEOUT;
    }

    /**
     * 결제 취소 처리
     */
    public void cancel() {
        if (this.paymentStatus != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                String.format("완료된 결제만 취소할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.CANCEL;
    }

    /**
     * 결제 환불 처리
     */
    public void refund() {
        if (this.paymentStatus != PaymentStatus.COMPLETED && this.paymentStatus != PaymentStatus.CANCEL) {
            throw new IllegalStateException(
                String.format("완료 또는 취소된 결제만 환불할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    @Override
    protected void guard() {
        if (this.amount != null && this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("결제 금액은 0보다 커야 합니다.");
        }
        if (this.paymentStatus == null) {
            throw new IllegalStateException("결제 상태는 필수입니다.");
        }
    }
}
