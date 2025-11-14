package com.loopers.domain.order;

import java.util.Optional;

/**
 * 주문 Repository 인터페이스
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderRepository {
    
    /**
     * 주문을 저장합니다.
     * 
     * @param order 저장할 주문 엔티티
     * @return 저장된 주문 엔티티
     */
    OrderEntity save(OrderEntity order);
    
    /**
     * 주문 ID로 주문을 조회합니다.
     * 
     * @param id 주문 ID
     * @return 조회된 주문 엔티티 (Optional)
     */
    Optional<OrderEntity> findById(Long id);
}
