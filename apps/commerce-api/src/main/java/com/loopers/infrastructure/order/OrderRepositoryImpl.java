package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 주문 Repository 구현체
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    
    private final OrderJpaRepository orderJpaRepository;
    
    @Override
    public OrderEntity save(OrderEntity order) {
        return orderJpaRepository.save(order);
    }
    
    @Override
    public Optional<OrderEntity> findById(Long id) {
        return orderJpaRepository.findById(id);
    }
}
