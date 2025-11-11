package com.loopers.domain.like;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */

@Component
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    @Transactional
    public LikeEntity upsertLikeProduct(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId)
                .map(like -> {
                    if(like.getDeletedAt() != null) {
                        like.restore();
                    }
                    return like;
                })
                .orElseGet(() -> likeRepository.save(LikeEntity.createEntity(userId, productId)));
    }

    @Transactional
    public void unlikeProduct(Long userId, Long productId) {
        LikeEntity likeEntity = likeRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_EXIST_LIKED));

        likeEntity.delete();

    }
}
