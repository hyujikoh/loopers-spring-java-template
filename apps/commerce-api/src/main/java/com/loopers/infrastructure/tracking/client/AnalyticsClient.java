package com.loopers.infrastructure.tracking.client;

import org.springframework.stereotype.Component;

import com.loopers.domain.tracking.event.UserBehaviorEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 행동 추적 로깅 시스템
 * 분석 시스템 API를 호출합니다.
 * 
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
public class AnalyticsClient {
    
    /**
     * 유저 행동 데이터를 분석 시스템에 전송
     * 
     * @param event 유저 행동 이벤트
     * @return 전송 성공 여부
     */
    public boolean sendBehaviorData(UserBehaviorEvent event) {
        try {
            // Fake 분석 시스템 API 호출 시뮬레이션
            log.info("📈 [ANALYTICS] 유저 행동 데이터 전송 시작 - eventType: {}, userId: {}, targetId: {}", 
                    event.eventType(), event.userId(), event.targetId());
            
            // 실제로는 HTTP 요청을 보냄
            // analyticsApi.track(event.userId(), event.eventType(), event.properties());
            // mixpanelClient.track(event.userId(), event.eventType(), event.properties());
            // amplitudeClient.logEvent(event.userId(), event.eventType(), event.properties());
            
            // 성공 시뮬레이션 (95% 성공률)
            if (Math.random() < 0.95) {
                Thread.sleep(50); // 네트워크 지연 시뮬레이션
                
                // 이벤트 타입별 로깅
                switch (event.eventType()) {
                    case "PRODUCT_VIEW" -> log.info("[ANALYTICS] 상품 조회 추적 완료 - productId: {}, userId: {}",
                            event.targetId(), event.userId());
                    case "PRODUCT_CLICK" -> log.info("[ANALYTICS] 상품 클릭 추적 완료 - productId: {}, userId: {}",
                            event.targetId(), event.userId());
                    case "LIKE_ACTION" -> log.info("[ANALYTICS] 좋아요 액션 추적 완료 - productId: {}, userId: {}, action: {}",
                            event.targetId(), event.userId(), event.properties().get("action"));
                    case "ORDER_CREATE" -> log.info("[ANALYTICS] 주문 생성 추적 완료 - orderId: {}, userId: {}, amount: {}",
                            event.targetId(), event.userId(), event.properties().get("totalAmount"));
                    default -> log.info("[ANALYTICS] 유저 행동 추적 완료 - eventType: {}, userId: {}",
                            event.eventType(), event.userId());
                }
                
                return true;
            } else {
                throw new CoreException(ErrorType.INTERNAL_ERROR, "분석 시스템 API 호출 실패 (시뮬레이션)");
            }
            
        } catch (Exception e) {
            log.error(" [ANALYTICS] 유저 행동 데이터 전송 실패 - eventType: {}, userId: {}, error: {}",
                    event.eventType(), event.userId(), e.getMessage());
            return false;
        }
    }
}
