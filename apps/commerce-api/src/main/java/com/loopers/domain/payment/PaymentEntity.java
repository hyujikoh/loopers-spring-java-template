package com.loopers.domain.payment;

import java.math.BigDecimal;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
public class PaymentEntity extends BaseEntity {
    private String  transactionKey;

    private String orderNumber;

    private BigDecimal amount;

    // TODO : 아직 카드를 enum 으로 할지 정하지 못함
    @Column(nullable = false, length = 20)
    private String cardType;

    // TODO : 마스킹된 카드 번호 에 대한 로직 필요
    @Column(nullable = false, length = 20)
    private String maskedCardNo;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;
}
