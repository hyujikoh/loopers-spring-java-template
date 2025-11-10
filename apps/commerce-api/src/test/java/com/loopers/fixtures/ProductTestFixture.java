package com.loopers.fixtures;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductDomainRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public class ProductTestFixture {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    public static ProductEntity createEntity(BrandEntity brand) {
        return createEntity(
                brand,
                "상품" + ID_GENERATOR.getAndIncrement(),
                "상품 설명",
                new BigDecimal("10000"),
                100
        );
    }

    public static ProductEntity createEntity(
            BrandEntity brand,
            String name,
            String description,
            BigDecimal price,
            int stock
    ) {
        ProductDomainRequest request = ProductDomainRequest.withoutDiscount(
                brand,
                name,
                description,
                price,
                stock
        );
        return ProductEntity.createEntity(request);
    }

    public static ProductEntity createAndSave(
            ProductRepository productRepository,
            BrandEntity brand,
            String name,
            String description,
            BigDecimal price,
            int stock
    ) {
        ProductEntity product = createEntity(brand, name, description, price, stock);
        return productRepository.save(product);
    }

    public static ProductEntity createAndSave(
            ProductRepository productRepository,
            BrandEntity brand
    ) {
        return createAndSave(
                productRepository,
                brand,
                "상품" + ID_GENERATOR.getAndIncrement(),
                "상품 설명",
                new BigDecimal("10000") ,
                100
        );
    }
}
