package com.loopers.domain.like;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 좋아요 통계 도메인 서비스
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeStatsService {
    
    private final ProductLikeStatsRepository statsRepository;
    private final LikeRepository likeRepository;
    
    /**
     * 상품 좋아요 통계를 조회하거나 생성한다.
     */
    @Transactional
    public ProductLikeStatsEntity getOrCreateStats(Long productId) {
        return statsRepository.findById(productId)
            .orElseGet(() -> {
                log.debug("상품 좋아요 통계 생성 - productId: {}", productId);
                ProductLikeStatsEntity stats = ProductLikeStatsEntity.create(productId);
                return statsRepository.save(stats);
            });
    }
    
    /**
     * 상품 좋아요 통계를 조회한다.
     */
    public ProductLikeStatsEntity getStats(Long productId) {
        return statsRepository.findById(productId).orElse(null);
    }
    
    /**
     * 좋아요 수를 증가시킨다.
     */
    @Transactional
    public void increaseLikeCount(Long productId) {
        log.debug("좋아요 수 증가 - productId: {}", productId);
        ProductLikeStatsEntity stats = getOrCreateStats(productId);
        stats.increaseLikeCount();
    }
    
    /**
     * 좋아요 수를 감소시킨다.
     */
    @Transactional
    public void decreaseLikeCount(Long productId) {
        log.debug("좋아요 수 감소 - productId: {}", productId);
        ProductLikeStatsEntity stats = statsRepository.findById(productId).orElse(null);
        
        if (stats != null) {
            stats.decreaseLikeCount();
        } else {
            log.warn("좋아요 통계를 찾을 수 없음 - productId: {}", productId);
        }
    }
    
    /**
     * 실제 좋아요 수와 통계를 동기화한다.
     */
    @Transactional
    public void syncStats(Long productId) {
        log.info("좋아요 통계 동기화 시작 - productId: {}", productId);
        
        Long actualCount = likeRepository.countByProductIdAndDeletedAtIsNull(productId);
        ProductLikeStatsEntity stats = getOrCreateStats(productId);
        
        Long beforeCount = stats.getLikeCount();
        stats.syncLikeCount(actualCount);
        
        log.info("좋아요 통계 동기화 완료 - productId: {}, before: {}, after: {}", 
                productId, beforeCount, actualCount);
    }
    
    /**
     * 여러 상품의 좋아요 수를 배치로 조회한다.
     * N+1 문제 방지를 위해 사용한다.
     */
    public Map<Long, Long> getLikeCountMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return statsRepository.findByProductIds(productIds)
            .stream()
            .collect(Collectors.toMap(
                ProductLikeStatsEntity::getProductId,
                ProductLikeStatsEntity::getLikeCount
            ));
    }
    
    /**
     * 상품의 좋아요 수를 조회한다.
     */
    public Long getLikeCount(Long productId) {
        ProductLikeStatsEntity stats = statsRepository.findById(productId).orElse(null);
        return stats != null ? stats.getLikeCount() : 0L;
    }
}
