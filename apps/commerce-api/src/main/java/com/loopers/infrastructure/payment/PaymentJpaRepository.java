package com.loopers.infrastructure.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.payment.PaymentEntity;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(String orderId);

    Optional<PaymentEntity> findByTransactionKey(String id);
}
