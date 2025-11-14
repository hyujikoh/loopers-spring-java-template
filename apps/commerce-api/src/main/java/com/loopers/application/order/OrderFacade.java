package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 Facade
 * 
 * <p>주문 생성, 확정, 취소 등의 유스케이스를 조정합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {
    
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;

    /**
     * 주문 생성
     * 
     * @param command 주문 생성 명령
     * @return 생성된 주문 정보
     * @throws IllegalArgumentException 재고 부족 또는 주문 불가능한 경우
     */
    @Transactional
    public OrderInfo createOrder(OrderCreateCommand command) {
        // 1. 사용자 조회
        UserEntity user = userService.getUserByUsername(command.username());
        
        // 2. 상품 조회 및 주문 가능 여부 검증
        List<ProductEntity> products = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemCommand itemCommand : command.orderItems()) {
            ProductEntity product = productService.getProductDetailLock(itemCommand.productId());
            
            // 엔티티의 canOrder 메서드로 주문 가능 여부 확인
            if (!product.canOrder(itemCommand.quantity())) {
                throw new IllegalArgumentException(
                    String.format("상품을 주문할 수 없습니다. (상품 ID: %d, 요청 수량: %d, 재고: %d)",
                        product.getId(), itemCommand.quantity(), product.getStockQuantity())
                );
            }
            
            products.add(product);
            
            // 엔티티의 getSellingPrice 메서드로 판매가 조회
            BigDecimal itemTotal = product.getSellingPrice()
                .multiply(BigDecimal.valueOf(itemCommand.quantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        // 3. 포인트 차감
        pointService.use(user, totalAmount);
        
        // 4. 주문 엔티티 생성 (정적 팩토리 메서드 사용)
        OrderEntity order = orderService.createOrder(
            new OrderDomainCreateRequest(user.getId(), totalAmount)
        );
        
        // 5. 주문 항목 생성 및 재고 차감
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (int i = 0; i < command.orderItems().size(); i++) {
            OrderItemCommand itemCommand = command.orderItems().get(i);
            ProductEntity product = products.get(i);
            
            // ProductService를 통한 재고 차감 (도메인 서비스 활용)
            productService.deductStock(product, itemCommand.quantity());
            
            // 주문 항목 엔티티 생성 (정적 팩토리 메서드 사용)
            OrderItemEntity orderItem = orderService.createOrderItem(
                new OrderItemDomainCreateRequest(
                    order.getId(),
                    product.getId(),
                    itemCommand.quantity(),
                    product.getSellingPrice()
                )
            );
            orderItems.add(orderItem);
        }
        
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 확정
     * 
     * <p>주문을 확정합니다. (재고는 이미 주문 생성 시 차감되었음)</p>
     * 
     * @param orderId 주문 ID
     * @return 확정된 주문 정보
     */
    @Transactional
    public OrderInfo confirmOrder(Long orderId) {
        // 1. 주문 확정
        OrderEntity order = orderService.getOrderById(orderId);
        order.confirmOrder();

        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);
        
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 취소
     * 
     * <p>주문을 취소하고 차감된 재고를 원복하며 포인트를 환불합니다.</p>
     * 
     * @param orderId 주문 ID
     * @param username 사용자명 (포인트 환불용)
     * @return 취소된 주문 정보
     */
    @Transactional
    public OrderInfo cancelOrder(Long orderId, String username) {
        // 1. 주문 취소
        OrderEntity order = orderService.getOrderById(orderId);
        order.cancelOrder();
        
        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);
        
        // 3. 재고 원복
        for (OrderItemEntity orderItem : orderItems) {
            productService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
        }
        
        // 4. 포인트 환불
        pointService.charge(username, order.getTotalAmount());
        
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 ID로 주문 조회
     * 
     * @param orderId 주문 ID
     * @return 주문 정보
     */
    public OrderInfo getOrderById(Long orderId) {
        OrderEntity order = orderService.getOrderById(orderId);
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 사용자 ID로 주문 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    public List<OrderInfo> getOrdersByUserId(Long userId) {
        List<OrderEntity> orders = orderService.getOrdersByUserId(userId);
        return orders.stream()
            .map(order -> {
                List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order.getId());
                return OrderInfo.from(order, orderItems);
            })
            .toList();
    }

    /**
     * 사용자 ID로 주문 목록 페이징 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    public org.springframework.data.domain.Page<OrderInfo> getOrdersByUserId(
            Long userId, 
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<OrderEntity> orderPage = 
                orderService.getOrdersByUserId(userId, pageable);
        
        return orderPage.map(order -> {
            List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order.getId());
            return OrderInfo.from(order, orderItems);
        });
    }

    /**
     * 사용자 ID와 주문 상태로 주문 목록 조회
     * 
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    public List<OrderInfo> getOrdersByUserIdAndStatus(Long userId, OrderStatus status) {
        List<OrderEntity> orders = orderService.getOrdersByUserIdAndStatus(userId, status);
        return orders.stream()
            .map(order -> {
                List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order.getId());
                return OrderInfo.from(order, orderItems);
            })
            .toList();
    }
    
    /**
     * 사용자 ID로 주문 요약 목록을 조회합니다.
     * 주문 항목 정보는 포함하지 않고 항목 개수만 포함합니다.
     *
     * @param userId 사용자 ID
     * @return 주문 요약 정보 목록
     */
    public List<OrderSummary> getOrderSummariesByUserId(Long userId) {
        List<OrderEntity> orders = orderService.getOrdersByUserId(userId);
        return orders.stream()
                .map(order -> {
                    int itemCount = orderService.countOrderItems(order.getId());
                    return OrderSummary.from(order, itemCount);
                })
                .toList();
    }
    
    /**
     * 주문 ID로 주문 요약 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 요약 정보
     */
    public OrderSummary getOrderSummaryById(Long orderId) {
        OrderEntity order = orderService.getOrderById(orderId);
        int itemCount = orderService.countOrderItems(orderId);
        return OrderSummary.from(order, itemCount);
    }
    
    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId 주문 ID
     * @param pageable 페이징 정보
     * @return 주문 항목 정보 페이지
     */
    public org.springframework.data.domain.Page<OrderItemInfo> getOrderItemsByOrderId(
            Long orderId, 
            org.springframework.data.domain.Pageable pageable) {
        // 주문 존재 여부 확인
        orderService.getOrderById(orderId);
        
        // 주문 항목 페이징 조회
        org.springframework.data.domain.Page<OrderItemEntity> orderItemsPage = 
                orderService.getOrderItemsByOrderId(orderId, pageable);
        return orderItemsPage.map(OrderItemInfo::from);
    }
}
