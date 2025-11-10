package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
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
        ProductPriceInfo price,
        BrandInfo brand
) {
    public static ProductDetailInfo of(ProductEntity entity) {
        return new ProductDetailInfo(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLikeCount(),
                new ProductPriceInfo(
                        entity.getPrice().getOriginPrice(),
                        entity.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        entity.getBrand().getId(),
                        entity.getBrand().getName(),
                        entity.getBrand().getDescription()
                )
        );
    }
}
