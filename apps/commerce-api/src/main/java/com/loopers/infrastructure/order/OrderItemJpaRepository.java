package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주문 항목 JPA Repository
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {
    
    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     * 
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    List<OrderItemEntity> findByOrderId(Long orderId);
}
