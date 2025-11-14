package com.loopers.application.order;

import lombok.Builder;

import java.util.List;

/**
 * 주문 생성 커맨드
 * 
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Builder
public record OrderCreateCommand(
        String username,
        List<OrderItemCommand> orderItems
) {
}
