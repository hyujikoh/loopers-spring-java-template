package com.loopers.interfaces.api.payment.client.dto;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
public record PgPaymentRequest(
        String orderNumber,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {
    public static PgPaymentRequest of(
            String orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String callbackUrl) {

        return new PgPaymentRequest(
                orderId,
                cardType,
                cardNo,
                amount,
                callbackUrl
        );
    }
}
