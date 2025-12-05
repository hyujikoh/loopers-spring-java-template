package com.loopers.application.payment;

import java.math.BigDecimal;

import lombok.Builder;

/**
 * 결제 명령 DTO
 * 
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Builder
public record PaymentCommand(
        String username,
        Long orderId,
        Long orderNumber,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {
}
