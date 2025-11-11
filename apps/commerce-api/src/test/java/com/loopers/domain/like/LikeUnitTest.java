package com.loopers.domain.like;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 단위 테스트")
class LikeUnitTest {
    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private LikeService likeService;

    @Nested
    @DisplayName("좋아요 등록")
    class like {

        @Test
        @DisplayName("좋아요 관계가 존재하지 않으면 새로 생성된다")
        void create_like_before_not_exist() {
            // given
            Long userId = 1L;
            Long productId = 2L;

            // when
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
            when(likeRepository.save(any(LikeEntity.class))).thenReturn(LikeEntity.createEntity(userId, productId));


            // then
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);
            assertNotNull(result);
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계가 존재하면 upsert로 복원된다")
        void 이미_삭제된_좋아요_관계가_존재하면_upsert로_복원된다() {
            // given
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity entity = LikeEntity.createEntity(userId, productId);
            entity.delete();

            // when
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(entity));

            // Then
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);
            assertNotNull(result);
            assertNull(result.getDeletedAt());  // upsert 로직에서 deletedAt이 null로 설정됨 검증

        }

        @Test
        @DisplayName("이미 존재하는 좋아요 관계이면 중복 방지로 등록을 무시한다")
        void 이미_존재하는_좋아요_관계이면_중복_방지로_등록을_무시한다() {
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity existingLike = LikeEntity.createEntity(userId, productId);
            // When
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingLike));

            // Then
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);
            assertEquals(existingLike, result);  // 기존 엔티티 반환 (upsert 무시)

        }

    }


    @Nested
    @DisplayName("좋아요 등록")
    class unlike {
        @Test
        @DisplayName("존재하는 좋아요 관계이면 취소에 성공한다")
        void 존재하는_좋아요_관계이면_취소에_성공한다() {
            // Given
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity existingLike = LikeEntity.createEntity(userId, productId);

            // When
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingLike));


            // 취소 로직에서 save 호출 가정
            likeService.unlikeProduct(userId, productId);
            assertNotNull(existingLike.getDeletedAt());  // 취소 후 deletedAt 설정 검증
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계이면 취소를 무시한다")
        void 이미_삭제된_좋아요_관계이면_취소를_무시한다() {
            // Given
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity deletedLike = LikeEntity.createEntity(userId, productId);
            deletedLike.delete();

            // When
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(deletedLike));

            // Then
            likeService.unlikeProduct(userId, productId);
            assertNotNull(deletedLike.getDeletedAt());
        }
    }


}
