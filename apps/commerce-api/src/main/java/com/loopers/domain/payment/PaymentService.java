package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.user.UserEntity;

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
    public PaymentEntity createPending(UserEntity user, PaymentCommand command) {
        PaymentEntity pending = PaymentEntity.createPending(
                user, command
        );
        return paymentRepository.save(pending);
    }

    @Transactional
    public PaymentEntity createFailedPayment(UserEntity user, PaymentCommand command, String reason) {
        PaymentEntity pending = PaymentEntity.createFailed(
                user, command, reason
        );
        return paymentRepository.save(pending);
    }

    @Transactional(readOnly = true)
    public PaymentEntity getByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다"));
    }

    /**
     * PENDING 상태로 특정 시간 이전에 생성된 결제 건 조회
     *
     * @param threshold 기준 시간
     * @return PENDING 상태의 오래된 결제 건 목록
     */
    @Transactional(readOnly = true)
    public List<PaymentEntity> findPendingPaymentsOlderThan(ZonedDateTime threshold) {
        return paymentRepository.findPendingPaymentsOlderThan(threshold);
    }
}
