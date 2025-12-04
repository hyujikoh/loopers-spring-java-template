package com.loopers.application.payment;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentCompletedEvent(
        String transactionKey,
        String orderId,
        Long userId,
        BigDecimal amount
) {}
