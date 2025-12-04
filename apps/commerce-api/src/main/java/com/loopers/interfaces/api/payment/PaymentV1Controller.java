package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.*;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1ApiSpec {
    private final PaymentFacade paymentFacade;

    @Override
    @PostMapping()
    public ApiResponse<PaymentV1Dtos.PaymentResponse> processPayment(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody PaymentV1Dtos.PaymentRequest request
    ) {
        PaymentCommand command = PaymentCommand.of(username, request);
        PaymentInfo info = paymentFacade.processPayment(command);
        return ApiResponse.success(PaymentV1Dtos.PaymentResponse.from(info));
    }


    /**
     * PG로부터 결제 결과 콜백 수신
     */
    @PostMapping("/callback")
    public ApiResponse<Void> handleCallback(
            @RequestBody PaymentV1Dtos.PgCallbackRequest request
    ) {
        log.info("PG 콜백 수신 - transactionKey: {}, status: {}",
                request.transactionKey(), request.status());

        paymentFacade.handlePaymentCallback(request);

        return ApiResponse.success(null);
    }

}
