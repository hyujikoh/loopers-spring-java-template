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
        BrandInfo brand
) {
    /**
     * ProductEntity와 BrandEntity를 조합하여 ProductDetailInfo를 생성한다.
     *
     * @param product 상품 엔티티
     * @param brand   브랜드 엔티티
     * @return ProductDetailInfo
     */
    public static ProductDetailInfo of(ProductEntity product, BrandEntity brand) {
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
                product.getLikeCount(),
                product.getStockQuantity(),
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        brand.getId(),
                        brand.getName(),
                        brand.getDescription()
                )
        );
    }
}
