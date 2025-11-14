package com.loopers.domain.order;

import java.math.BigDecimal;

/**
 * 주문 생성 도메인 요청 DTO
 * 
 * @param userId 사용자 ID
 * @param totalAmount 주문 총액
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderDomainCreateRequest(
        Long userId,
        BigDecimal totalAmount
) {
}
