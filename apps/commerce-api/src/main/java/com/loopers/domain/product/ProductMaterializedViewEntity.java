package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * 상품 Materialized View 엔티티
 * 
 * <p>상품, 브랜드, 좋아요 정보를 통합하여 조회 성능을 최적화하기 위한 MV 테이블입니다.
 * 실시간 업데이트가 아닌 배치 업데이트(2분 간격)를 통해 데이터를 동기화합니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Entity
@Table(name = "product_materialized_view", indexes = {
    @Index(name = "idx_pmv_brand_id", columnList = "brand_id"),
    @Index(name = "idx_pmv_like_count", columnList = "like_count"),
    @Index(name = "idx_pmv_brand_like", columnList = "brand_id, like_count"),
    @Index(name = "idx_pmv_name", columnList = "name"),
    @Index(name = "idx_pmv_updated_at", columnList = "last_updated_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMaterializedViewEntity extends BaseEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    // 상품 정보
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Price price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // 브랜드 정보
    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    // 좋아요 정보
    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    // 메타 정보
    @Column(name = "last_updated_at", nullable = false)
    private ZonedDateTime lastUpdatedAt;

    /**
     * ProductMaterializedViewEntity 생성자
     */
    private ProductMaterializedViewEntity(
            Long productId,
            String name,
            String description,
            Price price,
            Integer stockQuantity,
            Long brandId,
            String brandName,
            Long likeCount,
            ZonedDateTime lastUpdatedAt
    ) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.brandId = brandId;
        this.brandName = brandName;
        this.likeCount = likeCount;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /**
     * Product, Brand, 좋아요 수로부터 MV 엔티티를 생성합니다.
     * 
     * @param product 상품 엔티티
     * @param brand 브랜드 엔티티
     * @param likeCount 좋아요 수
     * @return 생성된 MV 엔티티
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public static ProductMaterializedViewEntity from(
            ProductEntity product,
            BrandEntity brand,
            Long likeCount
    ) {
        Objects.requireNonNull(product, "상품 엔티티는 필수입니다.");
        Objects.requireNonNull(brand, "브랜드 엔티티는 필수입니다.");
        Objects.requireNonNull(likeCount, "좋아요 수는 필수입니다.");

        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        return new ProductMaterializedViewEntity(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                brand.getId(),
                brand.getName(),
                likeCount,
                ZonedDateTime.now()
        );
    }

    /**
     * 기존 MV 엔티티를 최신 데이터로 동기화합니다.
     * 
     * @param product 최신 상품 엔티티
     * @param brand 최신 브랜드 엔티티
     * @param likeCount 최신 좋아요 수
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public void sync(ProductEntity product, BrandEntity brand, Long likeCount) {
        Objects.requireNonNull(product, "상품 엔티티는 필수입니다.");
        Objects.requireNonNull(brand, "브랜드 엔티티는 필수입니다.");
        Objects.requireNonNull(likeCount, "좋아요 수는 필수입니다.");

        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        if (!this.productId.equals(product.getId())) {
            throw new IllegalArgumentException(
                    String.format("상품 ID가 일치하지 않습니다. (MV: %d, Product: %d)", 
                            this.productId, product.getId())
            );
        }

        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.brandId = brand.getId();
        this.brandName = brand.getName();
        this.likeCount = likeCount;
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 마지막 배치 시간 이후 업데이트가 필요한지 확인합니다.
     * 
     * @param lastBatchTime 마지막 배치 실행 시간
     * @return 업데이트 필요 여부
     */
    public boolean needsUpdate(ZonedDateTime lastBatchTime) {
        if (lastBatchTime == null) {
            return true;
        }
        return this.lastUpdatedAt.isBefore(lastBatchTime);
    }

    @Override
    protected void guard() {
        if (this.productId == null) {
            throw new IllegalStateException("상품 ID는 필수입니다.");
        }

        if (this.name == null || this.name.isBlank()) {
            throw new IllegalStateException("상품명은 비어있을 수 없습니다.");
        }

        if (this.name.length() > 200) {
            throw new IllegalStateException("상품명은 200자를 초과할 수 없습니다.");
        }

        if (this.price == null) {
            throw new IllegalStateException("가격 정보는 필수입니다.");
        }

        if (this.stockQuantity == null || this.stockQuantity < 0) {
            throw new IllegalStateException("재고 수량은 0 이상이어야 합니다.");
        }

        if (this.brandId == null) {
            throw new IllegalStateException("브랜드 ID는 필수입니다.");
        }

        if (this.brandName == null || this.brandName.isBlank()) {
            throw new IllegalStateException("브랜드명은 비어있을 수 없습니다.");
        }

        if (this.brandName.length() > 100) {
            throw new IllegalStateException("브랜드명은 100자를 초과할 수 없습니다.");
        }

        if (this.likeCount == null || this.likeCount < 0) {
            throw new IllegalStateException("좋아요 수는 0 이상이어야 합니다.");
        }

        if (this.lastUpdatedAt == null) {
            throw new IllegalStateException("마지막 업데이트 시간은 필수입니다.");
        }
    }
}
