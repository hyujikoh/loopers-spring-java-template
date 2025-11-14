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
            ProductEntity product = productService.getProductDetail(itemCommand.productId());
            
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
            
            // 엔티티의 deductStock 메서드로 재고 차감
            product.deductStock(itemCommand.quantity());
            
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
}
