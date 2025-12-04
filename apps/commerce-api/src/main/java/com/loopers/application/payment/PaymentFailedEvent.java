package com.loopers.application.payment;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentFailedEvent(
        String transactionKey,
        String orderId,
        Long userId,
        String reason
) {}
