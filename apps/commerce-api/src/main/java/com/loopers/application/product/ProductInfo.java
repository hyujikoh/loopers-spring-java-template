package com.loopers.application.product;

import java.time.ZonedDateTime;

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
        Long brandId,
        ZonedDateTime createdAt
) {
    /**
     * ProductEntity와 좋아요 수를 조합하여 ProductInfo를 생성한다.
     *
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
     * @deprecated 좋아요 수를 명시적으로 전달하는 {@link #of(ProductEntity, Long)}를 사용하세요.
     * @param product 상품 엔티티
     * @return ProductInfo
     */
    @Deprecated
    public static ProductInfo of(ProductEntity product) {
        return of(product, 0L);
    }
}
