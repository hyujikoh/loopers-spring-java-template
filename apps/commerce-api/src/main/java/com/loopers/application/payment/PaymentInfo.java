package com.loopers.application.payment;

import java.math.BigDecimal;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentStatus;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentInfo(
        Long id,
        String transactionKey,
        String orderId,
        BigDecimal amount,
        PaymentStatus status
) {
    public static PaymentInfo from(PaymentEntity entity) {
        return new PaymentInfo(
                entity.getId(),
                entity.getTransactionKey(),
                entity.getOrderId(),
                entity.getAmount(),
                entity.getPaymentStatus()
        );
    }

    public static PaymentInfo pending(PaymentEntity entity) {
        return new PaymentInfo(
                entity.getId(),
                entity.getTransactionKey(),
                entity.getOrderId(),
                entity.getAmount(),
                PaymentStatus.PENDING
        );
    }
}
