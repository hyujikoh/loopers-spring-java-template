package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.ProductEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public record ProductInfo(
        Long id,
        String name,
        String description,
        Long likeCount,
        ProductPriceInfo price,
        BrandInfo brand
) {
    public static ProductInfo of(ProductEntity entity) {
        return new ProductInfo(
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
