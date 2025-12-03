package com.loopers.domain.payment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.user.UserEntity;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentDomainCreateRequest(
        Long userId,
        String orderId,
        String transactionKey,
        String cardType,
        String cardNo,
        String callbackUrl,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        ZonedDateTime requestedAt
) {
    public static PaymentDomainCreateRequest from(UserEntity user, PaymentCommand command, PgPaymentResponse pgResponse) {
        return new PaymentDomainCreateRequest(
                user.getId(),
                command.orderId(),
                pgResponse.transactionKey(),
                command.cardType(),
                command.cardNo(),
                command.callbackUrl(),
                command.amount(),
                PaymentStatus.valueOf(pgResponse.status()),
                ZonedDateTime.now()
        );
    }
}
