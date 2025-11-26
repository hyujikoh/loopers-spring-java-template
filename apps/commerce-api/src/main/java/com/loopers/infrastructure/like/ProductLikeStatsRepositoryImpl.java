package com.loopers.infrastructure.like;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.like.ProductLikeStatsEntity;
import com.loopers.domain.like.ProductLikeStatsRepository;

import lombok.RequiredArgsConstructor;

/**
 * 상품 좋아요 통계 Repository 구현체
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Repository
@RequiredArgsConstructor
public class ProductLikeStatsRepositoryImpl implements ProductLikeStatsRepository {
    
    private final ProductLikeStatsJpaRepository jpaRepository;
    
    @Override
    public ProductLikeStatsEntity save(ProductLikeStatsEntity stats) {
        return jpaRepository.save(stats);
    }
    
    @Override
    public Optional<ProductLikeStatsEntity> findById(Long productId) {
        return Optional.ofNullable(jpaRepository.findActiveById(productId));
    }
    
    @Override
    public List<ProductLikeStatsEntity> findByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return jpaRepository.findByProductIds(productIds);
    }
    
    @Override
    public boolean existsById(Long productId) {
        return jpaRepository.existsById(productId);
    }
    
    @Override
    public void deleteById(Long productId) {
        jpaRepository.deleteById(productId);
    }
}
