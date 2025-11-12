package com.loopers.domain.like;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 좋아요 도메인 서비스
 *
 * 좋아요 도메인의 비즈니스 로직을 처리합니다.
 * 단일 책임 원칙에 따라 좋아요 Repository에만 의존합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Component
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    /**
     * 사용자와 상품의 좋아요 관계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 좋아요 엔티티 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<LikeEntity> findLike(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId);
    }

    /**
     * 좋아요를 등록하거나 복원합니다 (Upsert).
     *
     * - 좋아요 관계가 없으면: 새로 생성
     * - 삭제된 좋아요가 있으면: 복원
     * - 활성 좋아요가 있으면: 기존 엔티티 반환 (중복 방지)
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 생성 또는 복원된 좋아요 엔티티
     */
    @Transactional
    public LikeEntity upsertLike(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId)
                .map(like -> {
                    // 삭제된 좋아요인 경우 복원
                    if (like.getDeletedAt() != null) {
                        like.restore();
                    }
                    return like;
                })
                // 좋아요가 없는 경우 새로 생성
                .orElseGet(() -> likeRepository.save(LikeEntity.createEntity(userId, productId)));
    }

    /**
     * 좋아요를 취소합니다 (소프트 삭제).
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    @Transactional
    public void unlikeProduct(Long userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(LikeEntity::delete);

    }
}
