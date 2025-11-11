package com.loopers.fixtures;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
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
                brand.getId(),
                "상품" + ID_GENERATOR.getAndIncrement(),
                "상품 설명",
                new BigDecimal("10000"),
                100
        );
    }

    public static ProductEntity createEntity(
            Long brandId,
            String name,
            String description,
            BigDecimal price,
            int stock
    ) {
        ProductDomainRequest request = ProductDomainRequest.withoutDiscount(
                brandId,
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
        ProductEntity product = createEntity(brand.getId(), name, description, price, stock);
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
                new BigDecimal("10000"),
                100
        );
    }

    /**
     * 브랜드와 상품을 생성하는 헬퍼 메서드
     *
     * @param brandRepository      브랜드 리포지토리
     * @param productRepository    상품 리포지토리
     * @param brandCount           브랜드 수
     * @param productCountPerBrand 브랜드당 상품 수
     */
    public static void createBrandsAndProducts(
            BrandRepository brandRepository,
            ProductRepository productRepository,
            int brandCount,
            int productCountPerBrand
    ) {
        List<BrandEntity> brands = BrandTestFixture.createEntities(brandCount)
                .stream()
                .map(brandRepository::save)
                .toList();

        brands.forEach(brand -> {
            IntStream.range(0, productCountPerBrand).forEach(i -> {
                ProductTestFixture.createAndSave(productRepository, brand);
            });
        });
    }
}
