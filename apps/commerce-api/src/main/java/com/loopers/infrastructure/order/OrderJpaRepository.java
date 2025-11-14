package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 JPA Repository
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
