package com.loopers.infrastructure.payment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

/**
 * PG Simulator Feign Client
 */
@FeignClient(
        name = "pgClient",
        url = "${pg.simulator.url}",
        configuration = PgClientConfig.class,
        fallbackFactory = PgClientFallbackFactory.class
)
public interface PgClient {

    /**
     * 결제 요청
     */
    @CircuitBreaker(name = "pgClient", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgClient")
    @TimeLimiter(name = "pgClient")
    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentRequest request
    );

    /**
     * 결제 정보 확인 (by transactionKey)
     */
    @CircuitBreaker(name = "pgClient", fallbackMethod = "getPaymentFallback")
    @Retry(name = "pgClient")
    @TimeLimiter(name = "pgClient")
    @GetMapping("/api/v1/payments/{transactionKey}")
    PgPaymentResponse getPayment(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable String transactionKey
    );

    /**
     * 결제 정보 목록 조회 (by orderId)
     */
    @CircuitBreaker(name = "pgClient", fallbackMethod = "getPaymentsByOrderIdFallback")
    @Retry(name = "pgClient")
    @TimeLimiter(name = "pgClient")
    @GetMapping("/api/v1/payments")
    List<PgPaymentResponse> getPaymentsByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String orderId
    );
}
