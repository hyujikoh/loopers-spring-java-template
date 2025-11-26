package com.loopers.application.like;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.like.LikeResult;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.ProductLikeStatsService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 애플리케이션 파사드
 * <p>
 * 좋아요 관련 유스케이스를 조정합니다.
 * 여러 도메인 서비스(User, Product, Like)를 조합하여 완전한 비즈니스 흐름을 구현합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final ProductService productService;
    private final UserService userService;
    private final LikeService likeService;
    private final ProductLikeStatsService statsService;
    private final ProductCacheService cacheService;

    /**
     * 좋아요를 등록하거나 복원합니다.
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @return 좋아요 정보 DTO
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public LikeInfo upsertLike(String username, Long productId) {
        log.debug("좋아요 등록/복원 시작 - username: {}, productId: {}", username, productId);
        
        // 1. 사용자 검증
        UserEntity user = userService.getUserByUsername(username);

        // 2. 상품 검증
        ProductEntity product = productService.getProductDetail(productId);

        // 3. 좋아요 등록/복원 (실제 변경 여부 확인)
        LikeResult result = likeService.upsertLike(user, product);

        // 4. 실제로 변경이 발생한 경우에만 통계 업데이트 및 캐시 처리
        if (result.changed()) {
            // 4-1. 좋아요 통계 업데이트 (MV 테이블)
            statsService.increaseLikeCount(productId);
            
            // 4-2. 캐시 업데이트 - 좋아요 수만 업데이트 (evict 대신 update)
            Long newLikeCount = statsService.getLikeCount(productId);
            cacheService.updateProductDetailLikeCount(productId, newLikeCount);
            
            // 4-3. 브랜드별 상품 ID 리스트 캐시 무효화 (정렬 순서 변경 가능성)
            cacheService.evictProductIdsByBrand(com.loopers.infrastructure.cache.CacheStrategy.HOT, product.getBrandId());
            
            log.info("좋아요 등록/복원 완료 (통계 업데이트) - username: {}, productId: {}, newLikeCount: {}", 
                    username, productId, newLikeCount);
        } else {
            log.debug("좋아요 이미 존재 (중복 요청) - username: {}, productId: {}", username, productId);
        }

        // 5. DTO 변환 후 반환
        return LikeInfo.of(result.entity(), product, user);
    }

    /**
     * 좋아요를 취소합니다.
     * <p>
     * 좋아요를 삭제하고 상품의 좋아요 카운트를 감소시킵니다.
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public void unlikeProduct(String username, Long productId) {
        log.debug("좋아요 취소 시작 - username: {}, productId: {}", username, productId);
        
        // 1. 사용자 검증
        UserEntity user = userService.getUserByUsername(username);

        // 2. 상품 검증
        ProductEntity product = productService.getProductDetail(productId);

        // 3. 좋아요 취소 (실제 변경 여부 확인)
        boolean changed = likeService.unlikeProduct(user, product);
        
        // 4. 실제로 변경이 발생한 경우에만 통계 업데이트 및 캐시 처리
        if (changed) {
            // 4-1. 좋아요 통계 업데이트 (MV 테이블)
            statsService.decreaseLikeCount(productId);
            
            // 4-2. 캐시 업데이트 - 좋아요 수만 업데이트 (evict 대신 update)
            Long newLikeCount = statsService.getLikeCount(productId);
            cacheService.updateProductDetailLikeCount(productId, newLikeCount);
            
            // 4-3. 브랜드별 상품 ID 리스트 캐시 무효화 (정렬 순서 변경 가능성)
            cacheService.evictProductIdsByBrand(com.loopers.infrastructure.cache.CacheStrategy.HOT, product.getBrandId());
            
            log.info("좋아요 취소 완료 (통계 업데이트) - username: {}, productId: {}, newLikeCount: {}", 
                    username, productId, newLikeCount);
        } else {
            log.debug("좋아요 이미 취소됨 (중복 요청) - username: {}, productId: {}", username, productId);
        }
    }
}
