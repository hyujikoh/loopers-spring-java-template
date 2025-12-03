package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.loopers.interfaces.api.ApiResponse;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentV1ApiSpec {

    ApiResponse<PaymentV1Dtos.PaymentResponse> processPayment(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody PaymentV1Dtos.PaymentRequest request
    );
}
