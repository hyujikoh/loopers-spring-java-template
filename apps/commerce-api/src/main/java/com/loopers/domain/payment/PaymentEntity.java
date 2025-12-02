package com.loopers.domain.payment;

import io.micrometer.core.annotation.Counted;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
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

    @Column(nullable = false, length = 20)
    private String transactionStatus;

    @Column(length = 20)
    private String failureReason;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;



}
