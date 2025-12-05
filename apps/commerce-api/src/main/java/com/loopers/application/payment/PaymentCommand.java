package com.loopers.application.payment;

import java.math.BigDecimal;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderInfo;

import lombok.Builder;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Builder
public record PaymentCommand(
        String username,
        Long orderId,
        String orderNumber,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {
}
