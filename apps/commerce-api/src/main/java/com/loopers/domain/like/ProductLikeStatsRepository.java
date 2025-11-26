package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

/**
 * 상품 좋아요 통계 Repository 인터페이스
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
public interface ProductLikeStatsRepository {
    
    /**
     * 상품 좋아요 통계를 저장한다.
     */
    ProductLikeStatsEntity save(ProductLikeStatsEntity stats);
    
    /**
     * 상품 ID로 좋아요 통계를 조회한다.
     */
    Optional<ProductLikeStatsEntity> findById(Long productId);
    
    /**
     * 여러 상품의 좋아요 통계를 배치로 조회한다.
     */
    List<ProductLikeStatsEntity> findByProductIds(List<Long> productIds);
    
    /**
     * 상품 좋아요 통계가 존재하는지 확인한다.
     */
    boolean existsById(Long productId);
    
    /**
     * 상품 좋아요 통계를 삭제한다.
     */
    void deleteById(Long productId);
}
