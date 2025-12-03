package com.loopers.infrastructure.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.payment.PaymentEntity;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
}
