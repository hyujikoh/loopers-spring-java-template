package com.loopers.domain.payment;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */

@Component
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    // 단일 도메인 비즈니스 로직만 처리
    public PaymentEntity createPayment() {
        return null;
    }
}
