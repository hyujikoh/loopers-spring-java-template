package com.loopers.domain.order;

import java.util.List;

/**
 * 주문 항목 Repository 인터페이스
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderItemRepository {
    
    /**
     * 주문 항목을 저장합니다.
     * 
     * @param orderItem 저장할 주문 항목 엔티티
     * @return 저장된 주문 항목 엔티티
     */
    OrderItemEntity save(OrderItemEntity orderItem);
    
    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     * 
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    List<OrderItemEntity> findByOrderId(Long orderId);
}
