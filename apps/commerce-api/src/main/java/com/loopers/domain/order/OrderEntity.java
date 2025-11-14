package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * 주문 엔티티
 * 
 * <p>사용자의 주문 정보를 관리하는 도메인 객체입니다.
 * 주문 생성, 확정, 총액 계산 등의 비즈니스 로직을 담당합니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_ordered_at", columnList = "ordered_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status;

    @Column(name = "ordered_at", nullable = false)
    private ZonedDateTime orderedAt;

    /**
     * 주문 엔티티 생성자
     * 
     * @param request 주문 생성 요청 DTO
     */
    private OrderEntity(OrderDomainCreateRequest request) {
        this.userId = request.userId();
        this.totalAmount = request.totalAmount();
        this.status = OrderStatus.PENDING;
        this.orderedAt = ZonedDateTime.now();
    }

    /**
     * 주문을 생성합니다.
     * 
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 엔티티
     */
    public static OrderEntity createOrder(OrderDomainCreateRequest request) {
        return new OrderEntity(request);
    }

    /**
     * 주문을 확정합니다.
     * PENDING 상태에서만 CONFIRMED로 변경 가능합니다.
     */
    public void confirmOrder() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(
                    ErrorType.INVALID_ORDER_STATUS,
                    String.format("주문 확정은 대기 상태에서만 가능합니다. (현재 상태: %s)", this.status)
            );
        }
        
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문이 대기 상태인지 확인합니다.
     * 
     * @return 대기 상태 여부
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * 주문이 확정 상태인지 확인합니다.
     * 
     * @return 확정 상태 여부
     */
    public boolean isConfirmed() {
        return this.status == OrderStatus.CONFIRMED;
    }

    @Override
    protected void guard() {
        if (this.userId == null) {
            throw new IllegalStateException("사용자 ID는 필수입니다.");
        }

        if (this.totalAmount == null || this.totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("주문 총액은 0보다 커야 합니다.");
        }

        if (this.status == null) {
            throw new IllegalStateException("주문 상태는 필수입니다.");
        }

        if (this.orderedAt == null) {
            throw new IllegalStateException("주문 일시는 필수입니다.");
        }
    }
}
