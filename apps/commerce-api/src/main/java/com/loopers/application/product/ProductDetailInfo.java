package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductEntity;

/**
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
     * ProductEntity, BrandEntity, 좋아요 수, 좋아요 여부를 조합하여 ProductDetailInfo를 생성한다.
     *
     * @param product 상품 엔티티
     * @param brand   브랜드 엔티티
     * @param likeCount 좋아요 수 (MV 테이블에서 조회)
     * @param isLiked 사용자의 좋아요 여부
     * @return ProductDetailInfo
     */
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
     * @deprecated 좋아요 수를 명시적으로 전달하는 {@link #of(ProductEntity, BrandEntity, Long, Boolean)}를 사용하세요.
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
     * @deprecated 좋아요 수를 명시적으로 전달하는 {@link #of(ProductEntity, BrandEntity, Long, Boolean)}를 사용하세요.
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
