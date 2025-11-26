package com.loopers.infrastructure.like;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.ProductLikeStatsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 좋아요 통계 동기화 스케줄러
 * 
 * <p>매일 새벽 3시에 실제 좋아요 수와 MV 테이블의 통계를 동기화합니다.</p>
 * <p>이를 통해 데이터 정합성을 보장합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeStatsSyncScheduler {
    
    private final ProductLikeStatsService statsService;
    private final LikeRepository likeRepository;
    
    /**
     * 전체 상품의 좋아요 통계를 동기화한다.
     * 
     * <p>매일 새벽 3시에 실행됩니다.</p>
     * <p>실제 좋아요 수와 MV 테이블의 통계가 일치하지 않는 경우 동기화합니다.</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncAllProductLikeStats() {
        log.info("=== 상품 좋아요 통계 전체 동기화 시작 ===");
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // 좋아요가 있는 모든 상품 ID 조회
            List<Long> productIds = likeRepository.findDistinctProductIds();
            
            log.info("동기화 대상 상품 수: {}", productIds.size());
            
            // 각 상품별로 동기화 수행
            for (Long productId : productIds) {
                try {
                    statsService.syncStats(productId);
                    successCount++;
                    
                    if (successCount % 100 == 0) {
                        log.info("동기화 진행 중... 성공: {}/{}", successCount, productIds.size());
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("상품 좋아요 통계 동기화 실패 - productId: {}, error: {}", 
                            productId, e.getMessage(), e);
                }
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.info("=== 상품 좋아요 통계 전체 동기화 완료 ===");
            log.info("총 대상: {}, 성공: {}, 실패: {}, 소요시간: {}ms", 
                    productIds.size(), successCount, failureCount, elapsedTime);
            
        } catch (Exception e) {
            log.error("상품 좋아요 통계 전체 동기화 중 오류 발생", e);
        }
    }
    
    /**
     * 특정 상품의 좋아요 통계를 동기화한다.
     * 
     * <p>수동 동기화가 필요한 경우 사용합니다.</p>
     * 
     * @param productId 동기화할 상품 ID
     */
    @Transactional
    public void syncProductLikeStats(Long productId) {
        log.info("상품 좋아요 통계 동기화 - productId: {}", productId);
        
        try {
            statsService.syncStats(productId);
            log.info("상품 좋아요 통계 동기화 완료 - productId: {}", productId);
        } catch (Exception e) {
            log.error("상품 좋아요 통계 동기화 실패 - productId: {}", productId, e);
            throw e;
        }
    }
}
