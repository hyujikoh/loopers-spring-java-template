package com.loopers.application.payment;

import java.math.BigDecimal;

import com.loopers.interfaces.api.payment.PaymentV1Dtos;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentCommand(
        String username,
        Long orderId,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {
    public static PaymentCommand of(String username, PaymentV1Dtos.PaymentRequest request) {
        return new PaymentCommand(
                username,
                request.orderId(),
                request.cardType(),
                request.cardNo(),
                request.amount(),
                request.callbackUrl()
        );
    }
}
