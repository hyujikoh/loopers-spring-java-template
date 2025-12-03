package com.loopers.domain.payment;

import java.util.List;

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

        PaymentEntity payment = PaymentEntity.createOrder(request);
        return paymentRepository.save(payment);
    }


    @Transactional
    public PaymentEntity createPending(PaymentCommand command) {
        PaymentEntity pending = PaymentEntity.createPending(
                command.orderId(),
                command.cardType(),
                command.cardNo(),
                command.amount(),
                command.callbackUrl()
        );
        return paymentRepository.save(pending);
    }

    public PaymentEntity getByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다"));
    }
}
