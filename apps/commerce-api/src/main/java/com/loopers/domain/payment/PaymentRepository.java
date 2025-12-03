package com.loopers.domain.payment;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentRepository {
    PaymentEntity save(PaymentEntity payment);

    Optional<PaymentEntity> findByOrderId(String orderId);

    Optional<PaymentEntity> findByTransactionKey(String transactionKey);
}
