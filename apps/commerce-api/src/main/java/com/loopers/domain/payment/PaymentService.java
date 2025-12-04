package com.loopers.domain.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.user.UserEntity;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */

@Component
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentEntity createPayment(UserEntity user, PaymentCommand command, PgPaymentResponse pgResponse) {
        PaymentDomainCreateRequest request = PaymentDomainCreateRequest.from(user, command, pgResponse);

        PaymentEntity payment = PaymentEntity.createPayment(request);
        return paymentRepository.save(payment);
    }


    @Transactional
    public PaymentEntity createPending(UserEntity user, PaymentCommand command) {
        PaymentEntity pending = PaymentEntity.createPending(
                user, command
        );
        return paymentRepository.save(pending);
    }

    public PaymentEntity getByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다"));
    }

    public PaymentEntity createFailedPayment(UserEntity user, PaymentCommand command, String reason) {
        PaymentEntity pending = PaymentEntity.crateFailed(
                user, command, reason
        );
        return paymentRepository.save(pending);
    }
}
