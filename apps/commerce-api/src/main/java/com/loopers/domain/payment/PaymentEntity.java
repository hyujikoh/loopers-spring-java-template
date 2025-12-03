package com.loopers.domain.payment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String transactionKey;

    @Column(nullable = false, length = 50)
    private String orderId;

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
     *  결제 생성 팩토리 메서드
     * @param request
     * @return
     */
    public static PaymentEntity createOrder(PaymentDomainCreateRequest request) {
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
        this.requestedAt = request.requestedAt();
    }

    public static PaymentEntity createPending(String orderId, String cardType, String cardNo, BigDecimal amount,
                                              String callbackUrl) {
        PaymentDomainCreateRequest request = new PaymentDomainCreateRequest(
                null,
                orderId,
                null,
                cardType,
                cardNo,
                callbackUrl,
                amount,
                PaymentStatus.PENDING,
                ZonedDateTime.now()
        );

        return new PaymentEntity(request);

    }
}
