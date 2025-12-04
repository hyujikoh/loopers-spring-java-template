package com.loopers.application.order;

import java.util.List;

import com.loopers.domain.payment.PaymentType;

import lombok.Builder;

/**
 * 주문 생성 커맨드
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Builder
public record OrderCreateCommand(
        String username,
        List<OrderItemCommand> orderItems,
        PaymentType paymentType,
        CardPaymentInfo cardInfo  // 카드 결제 시 사용
) {
    /**
     * 카드 결제 정보
     */
    public record CardPaymentInfo(
            String cardType,
            String cardNo,
            String callbackUrl
    ) {}
}
