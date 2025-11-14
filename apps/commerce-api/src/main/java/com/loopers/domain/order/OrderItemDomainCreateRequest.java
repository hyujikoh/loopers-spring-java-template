package com.loopers.domain.order;

import java.math.BigDecimal;

/**
 * 주문 항목 생성 도메인 요청 DTO
 * 
 * @param orderId 주문 ID
 * @param productId 상품 ID
 * @param quantity 수량
 * @param unitPrice 단가
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderItemDomainCreateRequest(
        Long orderId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
