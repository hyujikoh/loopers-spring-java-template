package com.loopers.infrastructure.like;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.like.ProductLikeStatsEntity;

/**
 * 상품 좋아요 통계 JPA Repository
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
public interface ProductLikeStatsJpaRepository extends JpaRepository<ProductLikeStatsEntity, Long> {
    
    /**
     * 여러 상품의 좋아요 통계를 배치로 조회한다.
     */
    @Query("SELECT s FROM ProductLikeStatsEntity s WHERE s.productId IN :productIds AND s.deletedAt IS NULL")
    List<ProductLikeStatsEntity> findByProductIds(@Param("productIds") List<Long> productIds);
    
    /**
     * 삭제되지 않은 통계만 조회한다.
     */
    @Query("SELECT s FROM ProductLikeStatsEntity s WHERE s.productId = :productId AND s.deletedAt IS NULL")
    ProductLikeStatsEntity findActiveById(@Param("productId") Long productId);
}
