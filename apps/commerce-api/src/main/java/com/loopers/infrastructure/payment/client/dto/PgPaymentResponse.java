package com.loopers.infrastructure.payment.client.dto;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
public record PgPaymentResponse(
        String transactionKey,
        String orderId,
        BigDecimal amount,
        String status,
        String message
) {
}
