package com.loopers.domain.order;

import java.util.List;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 도메인 서비스
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문을 생성합니다.
     * 
     * @param request 주문 생성 요청
     * @return 생성된 주문 엔티티
     */
    @Transactional
    public OrderEntity createOrder(OrderDomainCreateRequest request) {
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }
    
    /**
     * 주문 ID로 주문을 조회합니다.
     * 
     * @param orderId 주문 ID
     * @return 조회된 주문 엔티티
     * @throws CoreException 주문을 찾을 수 없는 경우
     */
    public OrderEntity getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("주문을 찾을 수 없습니다. (ID: %d)", orderId)
                ));
    }

    /**
     * 주문 항목을 생성합니다.
     *
     * @param request 주문 항목 생성 요청
     * @return 생성된 주문 항목 엔티티
     */
    @Transactional
    public OrderItemEntity createOrderItem(OrderItemDomainCreateRequest request) {
        OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);
        return orderItemRepository.save(orderItem);
    }

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    public List<OrderItemEntity> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
}
