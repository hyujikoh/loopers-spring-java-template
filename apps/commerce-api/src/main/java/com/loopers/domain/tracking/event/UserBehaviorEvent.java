package com.loopers.domain.tracking.event;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * 유저 행동 추적 이벤트
 * <p>
 * 상품 조회, 클릭, 좋아요, 주문 등 모든 유저 행동을 추적합니다.
 * 
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record UserBehaviorEvent(
        String eventType,           // "PRODUCT_VIEW", "PRODUCT_CLICK", "LIKE_ACTION", "ORDER_CREATE" 등
        Long userId,                // 사용자 ID (비로그인 시 null)
        String sessionId,           // 세션 ID (비로그인 사용자 추적용)
        String userAgent,           // 브라우저 정보
        String ipAddress,           // IP 주소
        Long targetId,              // 대상 ID (상품 ID, 주문 ID 등)
        String targetType,          // 대상 타입 ("PRODUCT", "ORDER", "USER" 등)
        Map<String, Object> properties, // 추가 속성 (검색어, 카테고리, 금액 등)
        ZonedDateTime eventTime,    // 이벤트 발생 시간
        String source              // 이벤트 발생 위치 ("WEB", "MOBILE_APP", "API" 등)
) {
    
    // 상품 조회 이벤트
    public static UserBehaviorEvent productView(
            Long userId, 
            String sessionId, 
            Long productId, 
            String userAgent, 
            String ipAddress,
            Map<String, Object> properties
    ) {
        return new UserBehaviorEvent(
                "PRODUCT_VIEW",
                userId,
                sessionId,
                userAgent,
                ipAddress,
                productId,
                "PRODUCT",
                properties,
                ZonedDateTime.now(),
                "WEB"
        );
    }
    
    // 상품 클릭 이벤트
    public static UserBehaviorEvent productClick(
            Long userId, 
            String sessionId, 
            Long productId, 
            String userAgent, 
            String ipAddress,
            Map<String, Object> properties
    ) {
        return new UserBehaviorEvent(
                "PRODUCT_CLICK",
                userId,
                sessionId,
                userAgent,
                ipAddress,
                productId,
                "PRODUCT",
                properties,
                ZonedDateTime.now(),
                "WEB"
        );
    }
    
    // 좋아요 액션 이벤트
    public static UserBehaviorEvent likeAction(
            Long userId, 
            String sessionId, 
            Long productId, 
            String action, // "LIKE" or "UNLIKE"
            String userAgent, 
            String ipAddress
    ) {
        return new UserBehaviorEvent(
                "LIKE_ACTION",
                userId,
                sessionId,
                userAgent,
                ipAddress,
                productId,
                "PRODUCT",
                Map.of("action", action),
                ZonedDateTime.now(),
                "WEB"
        );
    }
    
    // 주문 생성 이벤트
    public static UserBehaviorEvent orderCreate(
            Long userId, 
            String sessionId, 
            Long orderId, 
            String userAgent, 
            String ipAddress,
            Map<String, Object> properties
    ) {
        return new UserBehaviorEvent(
                "ORDER_CREATE",
                userId,
                sessionId,
                userAgent,
                ipAddress,
                orderId,
                "ORDER",
                properties,
                ZonedDateTime.now(),
                "WEB"
        );
    }
}