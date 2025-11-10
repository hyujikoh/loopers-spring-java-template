package com.loopers.domain.product;

import com.loopers.domain.brand.BrandEntity;

import java.math.BigDecimal;

/**
 * 상품 생성 요청 정보를 담는 레코드
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public record ProductDomainRequest(
    BrandEntity brand,
    String name,
    String description,
    BigDecimal originPrice,
    BigDecimal discountPrice,
    Integer stockQuantity
) {

    /**
     * 생성 요청 정보를 검증한다.
     */
    public ProductDomainRequest {
        if (brand == null) {
            throw new IllegalArgumentException("브랜드는 필수입니다.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException("상품명은 200자를 초과할 수 없습니다.");
        }

        if (originPrice == null || originPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정가는 0보다 커야 합니다.");
        }

        if (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인가는 음수일 수 없습니다.");
        }

        if (discountPrice != null && discountPrice.compareTo(originPrice) > 0) {
            throw new IllegalArgumentException("할인가는 정가보다 클 수 없습니다.");
        }

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
    }

    /**
     * 할인가 없이 상품 생성 요청을 만든다.
     *
     * @param brand 브랜드
     * @param name 상품명
     * @param description 상품 설명
     * @param originPrice 정가
     * @param stockQuantity 재고 수량
     * @return 상품 생성 요청
     */
    public static ProductDomainRequest withoutDiscount(
            BrandEntity brand,
            String name,
            String description,
            BigDecimal originPrice,
            Integer stockQuantity) {
        return new ProductDomainRequest(brand, name, description, originPrice, null, stockQuantity);
    }
}
