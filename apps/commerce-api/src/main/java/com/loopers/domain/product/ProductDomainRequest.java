package com.loopers.domain.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

/**
 * 상품 생성 요청 정보를 담는 레코드
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public record ProductDomainRequest(
        @NotNull
        Long brandId,
        @NotNull
        String name,

        String description,

        @NotNull
        BigDecimal originPrice,
        BigDecimal discountPrice,
        @NotNull
        Integer stockQuantity
) {
    /**
     * 할인가 없이 상품 생성 요청을 만든다.
     *
     * @param brandId       브랜드 ID
     * @param name          상품명
     * @param description   상품 설명
     * @param originPrice   정가
     * @param stockQuantity 재고 수량
     * @return 상품 생성 요청
     */
    public static ProductDomainRequest withoutDiscount(
            Long brandId,
            String name,
            String description,
            BigDecimal originPrice,
            Integer stockQuantity) {
        return new ProductDomainRequest(brandId, name, description, originPrice, null, stockQuantity);
    }
}
