package com.loopers.domain.payment;

import io.micrometer.core.annotation.Counted;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.BaseEntity;

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
    private String  transactionKey;

    @Column(nullable = false, length = 50)
    private String orderNumber;

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

}
