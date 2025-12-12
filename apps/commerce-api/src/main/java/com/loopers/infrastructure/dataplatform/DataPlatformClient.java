package com.loopers.infrastructure.dataplatform;

import org.springframework.stereotype.Component;

import com.loopers.infrastructure.dataplatform.dto.OrderDataDto;
import com.loopers.infrastructure.dataplatform.dto.PaymentDataDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 플랫폼 API 클라이언트 (Fake Implementation)
 * <p>
 * 실제 환경에서는 RestTemplate, WebClient, Feign 등을 사용하여 
 * 외부 데이터 플랫폼 API를 호출합니다.
 * 
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
public class DataPlatformClient {
    
    /**
     * 주문 데이터를 데이터 플랫폼에 전송
     * 
     * @param orderData 주문 데이터
     * @return 전송 성공 여부
     */
    public boolean sendOrderData(OrderDataDto orderData) {
        try {
            // Fake API 호출 시뮬레이션
            log.info("📊 [DATA PLATFORM] 주문 데이터 전송 시작 - orderId: {}, eventType: {}", 
                    orderData.orderId(), orderData.eventType());
            
            // 실제로는 HTTP 요청을 보냄
            // restTemplate.postForObject("https://data-platform.api/orders", orderData, String.class);
            
            // 성공 시뮬레이션 (90% 성공률)
            if (Math.random() < 0.9) {
                log.info("[DATA PLATFORM] 주문 데이터 전송 성공 - orderId: {}, orderNumber: {}",
                        orderData.orderId(), orderData.orderNumber());
                return true;
            } else {
                throw new CoreException(ErrorType.INTERNAL_ERROR,"데이터 플랫폼 API 호출 실패 (시뮬레이션)");
            }
            
        } catch (Exception e) {
            log.error(" [DATA PLATFORM] 주문 데이터 전송 실패 - orderId: {}, error: {}",
                    orderData.orderId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 결제 데이터를 데이터 플랫폼에 전송
     * 
     * @param paymentData 결제 데이터
     * @return 전송 성공 여부
     */
    public boolean sendPaymentData(PaymentDataDto paymentData) {
        try {
            // Fake API 호출 시뮬레이션
            log.info("📊 [DATA PLATFORM] 결제 데이터 전송 시작 - transactionKey: {}, eventType: {}", 
                    paymentData.transactionKey(), paymentData.eventType());
            
            // 실제로는 HTTP 요청을 보냄
            // restTemplate.postForObject("https://data-platform.api/payments", paymentData, String.class);
            
            // 성공 시뮬레이션 (90% 성공률)
            if (Math.random() < 0.9) {
                log.info("[DATA PLATFORM] 결제 데이터 전송 성공 - transactionKey: {}, orderId: {}",
                        paymentData.transactionKey(), paymentData.orderId());
                return true;
            } else {
                throw new CoreException(ErrorType.INTERNAL_ERROR, "데이터 플랫폼 API 호출 실패 (시뮬레이션)");
            }
            
        } catch (Exception e) {
            log.error(" [DATA PLATFORM] 결제 데이터 전송 실패 - transactionKey: {}, error: {}",
                    paymentData.transactionKey(), e.getMessage());
            return false;
        }
    }
}
