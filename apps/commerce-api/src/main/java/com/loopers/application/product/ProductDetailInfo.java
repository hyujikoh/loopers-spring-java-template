package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMaterializedViewEntity;

/**
 * 상품 상세 정보 DTO
 * 
 * <p>상품 상세 조회 시 사용되는 DTO입니다.</p>
 * <p>ProductMaterializedViewEntity로부터 생성하는 것을 권장합니다 (성능 최적화).</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public record ProductDetailInfo(
        Long id,
        String name,
        String description,
        Long likeCount,
        Integer stockQuantity,
        ProductPriceInfo price,
        BrandInfo brand,
        Boolean isLiked  // 사용자의 좋아요 여부 (false: 비로그인 또는 좋아요 안함 , true: 좋아요함)
) {

    /**
     * ProductMaterializedViewEntity와 좋아요 여부를 조합하여 ProductDetailInfo를 생성한다.
     * 
     * <p>MV 테이블은 상품, 브랜드, 좋아요 정보를 통합하여 조인 없이 빠른 조회가 가능합니다.</p>
     * 
     * @param mv Materialized View 엔티티
     * @param isLiked 사용자의 좋아요 여부
     * @return ProductDetailInfo
     */
    public static ProductDetailInfo from(ProductMaterializedViewEntity mv, Boolean isLiked) {
        if (mv == null) {
            throw new IllegalArgumentException("MV 엔티티는 필수입니다.");
        }

        return new ProductDetailInfo(
                mv.getProductId(),
                mv.getName(),
                mv.getDescription(),
                mv.getLikeCount(),
                mv.getStockQuantity(),
                new ProductPriceInfo(
                        mv.getPrice().getOriginPrice(),
                        mv.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        mv.getBrandId(),
                        mv.getBrandName(),
                        null // MV에는 브랜드 설명이 없으므로 null
                ),
                isLiked
        );
    }

    /**
     * ProductEntity, BrandEntity, 좋아요 수, 좋아요 여부를 조합하여 ProductDetailInfo를 생성한다.
     *
     * @deprecated MV 테이블을 사용하는 {@link #from(ProductMaterializedViewEntity, Boolean)}를 사용하세요.
     * @param product 상품 엔티티
     * @param brand   브랜드 엔티티
     * @param likeCount 좋아요 수 (MV 테이블에서 조회)
     * @param isLiked 사용자의 좋아요 여부
     * @return ProductDetailInfo
     */
    @Deprecated
    public static ProductDetailInfo of(ProductEntity product, BrandEntity brand, Long likeCount, Boolean isLiked) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }

        if (brand == null) {
            throw new IllegalArgumentException("브랜드 정보는 필수입니다.");
        }

        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                likeCount != null ? likeCount : 0L,
                product.getStockQuantity(),
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        brand.getId(),
                        brand.getName(),
                        brand.getDescription()
                ),
                isLiked
        );
    }
    
    /**
     * ProductEntity와 BrandEntity를 조합하여 ProductDetailInfo를 생성한다.
     * 좋아요 수는 0, 좋아요 여부는 false로 기본 설정된다.
     *
     * @deprecated MV 테이블을 사용하는 {@link #from(ProductMaterializedViewEntity, Boolean)}를 사용하세요.
     * @param product 상품 엔티티
     * @param brand   브랜드 엔티티
     * @return ProductDetailInfo
     */
    @Deprecated
    public static ProductDetailInfo of(ProductEntity product, BrandEntity brand) {
        return of(product, brand, 0L, false);
    }

    /**
     * ProductEntity, BrandEntity, 좋아요 여부를 조합하여 ProductDetailInfo를 생성한다.
     * 좋아요 수는 0으로 기본 설정된다.
     *
     * @deprecated MV 테이블을 사용하는 {@link #from(ProductMaterializedViewEntity, Boolean)}를 사용하세요.
     * @param product 상품 엔티티
     * @param brand   브랜드 엔티티
     * @param isLiked 사용자의 좋아요 여부
     * @return ProductDetailInfo
     */
    @Deprecated
    public static ProductDetailInfo of(ProductEntity product, BrandEntity brand, Boolean isLiked) {
        return of(product, brand, 0L, isLiked);
    }
}
