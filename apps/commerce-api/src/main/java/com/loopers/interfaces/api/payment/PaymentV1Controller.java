package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1ApiSpec {
    private final PaymentFacade paymentFacade;

    @Override
    public ApiResponse<PaymentV1Dtos.PaymentResponse> processPayment(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody PaymentV1Dtos.PaymentRequest request
    ) {
        PaymentCommand command = PaymentCommand.of(username, request);
        PaymentInfo info = paymentFacade.processPayment(command);
        return ApiResponse.success(PaymentV1Dtos.PaymentResponse.from(info));
    }
}
