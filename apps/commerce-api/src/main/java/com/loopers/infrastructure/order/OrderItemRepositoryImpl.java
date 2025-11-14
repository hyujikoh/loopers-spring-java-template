package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 주문 항목 Repository 구현체
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {
    
    private final OrderItemJpaRepository orderItemJpaRepository;
    
    @Override
    public OrderItemEntity save(OrderItemEntity orderItem) {
        return orderItemJpaRepository.save(orderItem);
    }
    
    @Override
    public List<OrderItemEntity> findByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }
}
