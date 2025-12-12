package com.loopers.domain.tracking;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.loopers.domain.tracking.event.UserBehaviorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 행동 추적기
 * <p>
 * 유저의 모든 행동을 추적하고 이벤트를 발행합니다.
 * 비즈니스 로직에 영향을 주지 않도록 비동기로 처리됩니다.
 * 
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserBehaviorTracker {
    
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 상품 조회 추적
     */
    public void trackProductView(
            Long userId, 
            String sessionId, 
            Long productId, 
            String userAgent, 
            String ipAddress,
            String category,
            String searchKeyword
    ) {
        try {
            Map<String, Object> properties = Map.of(
                    "category", category != null ? category : "",
                    "searchKeyword", searchKeyword != null ? searchKeyword : ""
            );
            
            UserBehaviorEvent event = UserBehaviorEvent.productView(
                    userId, sessionId, productId, userAgent, ipAddress, properties
            );
            
            eventPublisher.publishEvent(event);
            log.debug("상품 조회 추적 - userId: {}, productId: {}", userId, productId);
            
        } catch (Exception e) {
            // 추적 실패가 비즈니스 로직에 영향 주지 않도록
            log.warn("상품 조회 추적 실패 - userId: {}, productId: {}, error: {}", 
                    userId, productId, e.getMessage());
        }
    }
    
    /**
     * 상품 클릭 추적
     */
    public void trackProductClick(
            Long userId, 
            String sessionId, 
            Long productId, 
            String userAgent, 
            String ipAddress,
            String clickPosition,
            String referrer
    ) {
        try {
            Map<String, Object> properties = Map.of(
                    "clickPosition", clickPosition != null ? clickPosition : "",
                    "referrer", referrer != null ? referrer : ""
            );
            
            UserBehaviorEvent event = UserBehaviorEvent.productClick(
                    userId, sessionId, productId, userAgent, ipAddress, properties
            );
            
            eventPublisher.publishEvent(event);
            log.debug("상품 클릭 추적 - userId: {}, productId: {}", userId, productId);
            
        } catch (Exception e) {
            log.warn("상품 클릭 추적 실패 - userId: {}, productId: {}, error: {}", 
                    userId, productId, e.getMessage());
        }
    }
    
    /**
     * 좋아요 액션 추적
     */
    public void trackLikeAction(
            Long userId, 
            String sessionId, 
            Long productId, 
            String action,
            String userAgent, 
            String ipAddress
    ) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.likeAction(
                    userId, sessionId, productId, action, userAgent, ipAddress
            );
            
            eventPublisher.publishEvent(event);
            log.debug("좋아요 액션 추적 - userId: {}, productId: {}, action: {}", 
                    userId, productId, action);
            
        } catch (Exception e) {
            log.warn("좋아요 액션 추적 실패 - userId: {}, productId: {}, action: {}, error: {}", 
                    userId, productId, action, e.getMessage());
        }
    }
    
    /**
     * 주문 생성 추적
     */
    public void trackOrderCreate(
            Long userId, 
            String sessionId, 
            Long orderId, 
            String userAgent, 
            String ipAddress,
            String paymentMethod,
            Double totalAmount,
            Integer itemCount
    ) {
        try {
            Map<String, Object> properties = Map.of(
                    "paymentMethod", paymentMethod != null ? paymentMethod : "",
                    "totalAmount", totalAmount != null ? totalAmount : 0.0,
                    "itemCount", itemCount != null ? itemCount : 0
            );
            
            UserBehaviorEvent event = UserBehaviorEvent.orderCreate(
                    userId, sessionId, orderId, userAgent, ipAddress, properties
            );
            
            eventPublisher.publishEvent(event);
            log.debug("주문 생성 추적 - userId: {}, orderId: {}", userId, orderId);
            
        } catch (Exception e) {
            log.warn("주문 생성 추적 실패 - userId: {}, orderId: {}, error: {}", 
                    userId, orderId, e.getMessage());
        }
    }
}