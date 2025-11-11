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
 * LikeService의 단위 테스트 클래스
 *
 * 좋아요 등록, 취소 기능을 테스트하며, 중복 방지 및 멱등성 처리를 검증합니다.
 *
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

    /**
     * 좋아요 등록 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 등록")
    class LikeRegistration {

        @Test
        @DisplayName("좋아요 관계가 존재하지 않으면 새로 생성된다")
        void create_like_when_not_exist() {
            // Given: 좋아요 관계가 존재하지 않는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
            when(likeRepository.save(any(LikeEntity.class))).thenReturn(LikeEntity.createEntity(userId, productId));

            // When: 좋아요 등록 메서드 호출
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);

            // Then: 새로운 좋아요 엔티티가 생성되었음을 검증
            assertNotNull(result);
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계가 존재하면 upsert로 복원된다")
        void restore_deleted_like_when_exist() {
            // Given: 삭제된 좋아요 엔티티가 존재하는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity entity = LikeEntity.createEntity(userId, productId);
            entity.delete(); // 삭제 상태로 설정
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(entity));

            // When: 좋아요 등록 메서드 호출 (upsert)
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);

            // Then: 삭제된 엔티티가 복원되었음을 검증 (deletedAt이 null)
            assertNotNull(result);
            assertNull(result.getDeletedAt(), "upsert 로직에서 deletedAt이 null로 설정되어야 함");
        }

        /**
         * 이미 존재하는 좋아요 관계일 때 중복 방지로 등록을 무시하는지 테스트
         */
        @Test
        @DisplayName("이미 존재하는 좋아요 관계이면 중복 방지로 등록을 무시한다")
        void ignore_duplicate_like_when_exist() {
            // Given: 활성 상태의 좋아요 엔티티가 존재하는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity existingLike = LikeEntity.createEntity(userId, productId);
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingLike));

            // When: 좋아요 등록 메서드 호출
            LikeEntity result = likeService.upsertLikeProduct(userId, productId);

            // Then: 기존 엔티티가 반환되고 중복 생성되지 않음을 검증
            assertEquals(existingLike, result, "기존 엔티티가 반환되어야 함 (upsert 무시)");
        }
    }

    /**
     * 좋아요 취소 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 취소")
    class LikeCancellation {

        @Test
        @DisplayName("존재하지 않는 좋아요 관계이면 취소를 무시한다")
        void ignore_cancel_when_not_exist() {
            // Given: 좋아요 관계가 존재하지 않는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(userId, productId);

            // Then: 예외 없이 무시됨 (별도 검증 없음)
        }

        @Test
        @DisplayName("존재하는 좋아요 관계이면 취소에 성공한다")
        void cancel_like_when_exist() {
            // Given: 활성 상태의 좋아요 엔티티가 존재하는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity existingLike = LikeEntity.createEntity(userId, productId);
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingLike));

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(userId, productId);

            // Then: 엔티티가 삭제 상태로 변경되었음을 검증
            assertNotNull(existingLike.getDeletedAt(), "취소 후 deletedAt이 설정되어야 함");
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계이면 취소를 무시한다")
        void ignore_cancel_when_already_deleted() {
            // Given: 이미 삭제된 좋아요 엔티티가 존재하는 상황 설정
            Long userId = 1L;
            Long productId = 2L;
            LikeEntity deletedLike = LikeEntity.createEntity(userId, productId);
            deletedLike.delete(); // 이미 삭제 상태
            when(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(deletedLike));

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(userId, productId);

            // Then: 이미 삭제된 상태이므로 변경 없음
            assertNotNull(deletedLike.getDeletedAt(), "이미 삭제된 상태여야 함");
        }
    }
}
