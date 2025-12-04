package com.loopers.interfaces.api.payment;

import java.math.BigDecimal;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentStatus;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public class PaymentV1Dtos {
    public record PaymentRequest(
            Long orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String callbackUrl
    ) {}

    public record PaymentResponse(
            String transactionKey,
            Long orderId,
            BigDecimal amount,
            PaymentStatus status
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                    info.transactionKey(),
                    info.orderId(),
                    info.amount(),
                    info.status()
            );
        }
    }

    public record PgCallbackRequest(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String status,
            String reason
    ) {
    }
}

