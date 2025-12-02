package com.loopers.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
@AllArgsConstructor
@Getter
public enum PaymentType {
    CARD("카드 결제"),
    REFUND("환불");

    private final String description;
}
