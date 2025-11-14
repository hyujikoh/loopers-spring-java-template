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
     */
    @Transactional
    public OrderInfo createOrder(OrderCreateCommand command) {
        // 1. 사용자 조회
        UserEntity user = userService.getUserByUsername(command.username());
        
        // 2. 상품 조회 및 총액 계산 (주문 생성 전에 먼저 계산)
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ProductEntity> products = new ArrayList<>();
        
        for (OrderItemCommand itemCommand : command.orderItems()) {
            // 상품 조회
            ProductEntity product = productService.getProductDetail(itemCommand.productId());
            
            // 재고 확인
            if (product.getStockQuantity() < itemCommand.quantity()) {
                throw new IllegalArgumentException(
                    String.format("상품 재고가 부족합니다. (상품 ID: %d, 요청 수량: %d, 재고: %d)",
                        product.getId(), itemCommand.quantity(), product.getStockQuantity())
                );
            }
            
            products.add(product);
            
            // 총액 계산
            BigDecimal itemTotal = product.getPrice().getSellingPrice()
                .multiply(BigDecimal.valueOf(itemCommand.quantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        // 3. 포인트 차감 (주문 생성 전에 먼저 차감)
        pointService.use(user, totalAmount);
        
        // 4. 주문 생성 (계산된 총액으로 생성)
        OrderEntity order = orderService.createOrder(
            new OrderDomainCreateRequest(user.getId(), totalAmount)
        );
        
        // 5. 주문 항목 생성 및 재고 차감
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (int i = 0; i < command.orderItems().size(); i++) {
            OrderItemCommand itemCommand = command.orderItems().get(i);
            ProductEntity product = products.get(i);
            
            // 재고 차감
            product.deductStock(itemCommand.quantity());
            
            // 주문 항목 생성
            OrderItemEntity orderItem = orderService.createOrderItem(
                new OrderItemDomainCreateRequest(
                    order.getId(),
                    product.getId(),
                    itemCommand.quantity(),
                    product.getPrice().getSellingPrice()
                )
            );
            orderItems.add(orderItem);
        }
        
        return OrderInfo.from(order, orderItems);
    }
}
