package com.loopers.domain.like;

import java.time.ZonedDateTime;

import com.loopers.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Entity
@Table(name = "product_like_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLikeStatsEntity  {

    @Id
    private Long productId;  // 단순 외래키

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "last_updated_at", nullable = false)
    private ZonedDateTime lastUpdatedAt;


    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @PrePersist
    private void prePersist() {
        guard();

        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        guard();

        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * delete 연산은 멱등하게 동작할 수 있도록 한다. (삭제된 엔티티를 다시 삭제해도 동일한 결과가 나오도록)
     */
    public void delete() {
        if (this.deletedAt == null) {
            this.deletedAt = ZonedDateTime.now();
        }
    }

    /**
     * restore 연산은 멱등하게 동작할 수 있도록 한다. (삭제되지 않은 엔티티를 복원해도 동일한 결과가 나오도록)
     */
    public void restore() {
        if (this.deletedAt != null) {
            this.deletedAt = null;
        }
    }


    // 생성자
    private ProductLikeStatsEntity(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    // 정적 팩토리 메서드
    public static ProductLikeStatsEntity create(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        return new ProductLikeStatsEntity(productId);
    }

    // 비즈니스 로직
    public void increaseLikeCount() {
        this.likeCount++;
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
            this.lastUpdatedAt = ZonedDateTime.now();
        }
    }

    public void syncLikeCount(Long actualCount) {
        if (actualCount == null || actualCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }
        this.likeCount = actualCount;
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    protected void guard() {
        if (this.productId == null) {
            throw new IllegalStateException("상품 ID는 필수입니다.");
        }
        if (this.likeCount == null || this.likeCount < 0) {
            throw new IllegalStateException("좋아요 수는 0 이상이어야 합니다.");
        }
    }
}
