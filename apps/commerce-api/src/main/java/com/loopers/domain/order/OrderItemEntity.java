package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 항목 엔티티
 * 
 * <p>주문에 포함된 개별 상품 정보를 관리하는 도메인 객체입니다.
 * 주문 시점의 상품 가격을 스냅샷으로 저장하여 가격 변동에 영향받지 않습니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order_id", columnList = "order_id"),
        @Index(name = "idx_order_item_product_id", columnList = "product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    /**
     * 주문 항목 엔티티 생성자
     * 
     * @param request 주문 항목 생성 요청 DTO
     */
    private OrderItemEntity(OrderItemDomainCreateRequest request) {
        this.orderId = request.orderId();
        this.productId = request.productId();
        this.quantity = request.quantity();
        this.unitPrice = request.unitPrice();
        this.totalPrice = calculateItemTotal();
    }

    /**
     * 주문 항목을 생성합니다.
     * 
     * @param request 주문 항목 생성 요청 DTO
     * @return 생성된 주문 항목 엔티티
     */
    public static OrderItemEntity createOrderItem(OrderItemDomainCreateRequest request) {
        return new OrderItemEntity(request);
    }

    /**
     * 주문 항목의 총액을 계산합니다.
     * 
     * @return 항목 총액 (단가 × 수량)
     */
    public BigDecimal calculateItemTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    protected void guard() {
        if (this.orderId == null) {
            throw new IllegalStateException("주문 ID는 필수입니다.");
        }

        if (this.productId == null) {
            throw new IllegalStateException("상품 ID는 필수입니다.");
        }

        if (this.quantity == null || this.quantity <= 0) {
            throw new IllegalStateException("주문 수량은 1 이상이어야 합니다.");
        }

        if (this.unitPrice == null || this.unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("단가는 0보다 커야 합니다.");
        }

        if (this.totalPrice == null || this.totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("총 가격은 0보다 커야 합니다.");
        }

        // 총 가격 일치성 검증
        BigDecimal calculatedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (this.totalPrice.compareTo(calculatedTotal) != 0) {
            throw new IllegalStateException("총 가격이 단가 × 수량과 일치하지 않습니다.");
        }
    }
}
