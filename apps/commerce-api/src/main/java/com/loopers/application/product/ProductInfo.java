package com.loopers.application.product;

import java.time.ZonedDateTime;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMaterializedViewEntity;

/**
 * 상품 정보 DTO
 * 
 * <p>상품 목록 조회 시 사용되는 DTO입니다.</p>
 * <p>ProductMaterializedViewEntity로부터 생성하는 것을 권장합니다 (성능 최적화).</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public record ProductInfo(
        Long id,
        String name,
        String description,
        Long likeCount,
        ProductPriceInfo price,
        Long brandId,
        ZonedDateTime createdAt
) {
    /**
     * ProductMaterializedViewEntity로부터 ProductInfo를 생성한다.
     * 
     * <p>MV 테이블은 상품, 브랜드, 좋아요 정보를 통합하여 조인 없이 빠른 조회가 가능합니다.</p>
     * 
     * @param mv Materialized View 엔티티
     * @return ProductInfo
     */
    public static ProductInfo from(ProductMaterializedViewEntity mv) {
        if (mv == null) {
            throw new IllegalArgumentException("MV 엔티티는 필수입니다.");
        }

        return new ProductInfo(
                mv.getProductId(),
                mv.getName(),
                mv.getDescription(),
                mv.getLikeCount(),
                new ProductPriceInfo(
                        mv.getPrice().getOriginPrice(),
                        mv.getPrice().getDiscountPrice()
                ),
                mv.getBrandId(),
                mv.getCreatedAt()
        );
    }
    
    /**
     * ProductEntity와 좋아요 수를 조합하여 ProductInfo를 생성한다.
     *
     * @deprecated MV 테이블을 사용하는 {@link #from(ProductMaterializedViewEntity)}를 사용하세요.
     * @param product 상품 엔티티
     * @param likeCount 좋아요 수 (MV 테이블에서 조회)
     * @return ProductInfo
     */
    public static ProductInfo of(ProductEntity product, Long likeCount) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }

        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                likeCount != null ? likeCount : 0L,
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                product.getBrandId(),
                product.getCreatedAt()
        );
    }
    
    /**
     * ProductEntity로부터 ProductInfo를 생성한다 (좋아요 수 0으로 초기화).
     * 
     * @deprecated MV 테이블을 사용하는 {@link #from(ProductMaterializedViewEntity)}를 사용하세요.
     * @param product 상품 엔티티
     * @return ProductInfo
     */
    public static ProductInfo of(ProductEntity product) {
        return of(product, 0L);
    }
}
