package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@SpringBootTest
@DisplayName("Product 통합 테스트")
public class ProductIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductFacade productFacade;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("상품 저장 및 조회")
    class ListProductsTest {

        @Test
        @DisplayName("새로운 상품 등록 시 상품이 저장된다")
        void save_product_with_brand() {
            // given
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Nike", "Just do it");

            // when
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Air Max",
                    "편안한 운동화",
                    new BigDecimal("120000"),
                    50
            );

            // then
            assertThat(product.getId()).isNotNull();
            assertThat(product.getName()).isEqualTo("Air Max");
            assertThat(product.getBrand().getId()).isEqualTo(brand.getId());
        }


        @org.junit.jupiter.api.Test
        void get_product_pageingnation() {
            // given
            List<BrandEntity> brands = BrandTestFixture.createEntities(2)
                    .stream()
                    .map(brandRepository::save)
                    .toList();

            // 각 브랜드당 5개의 상품 생성
            brands.forEach(brand -> {
                IntStream.range(0, 5).forEach(i -> {
                    ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);

                    productRepository.save(product);
                });
            });

            Pageable pageable = PageRequest.of(0, 5);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(pageable);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(5);
            assertThat(productInfos.getTotalElements()).isEqualTo(10);

            ProductInfo firstProduct = productInfos.getContent().get(0);
            assertThat(firstProduct.name()).isNotNull();
            assertThat(firstProduct.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(firstProduct.brand()).isNotNull();
        }


        @Test
        void get_product_detail() {
            // given
            List<BrandEntity> brands = BrandTestFixture.createEntities(2)
                    .stream()
                    .map(brandRepository::save)
                    .toList();

            // 각 브랜드당 5개의 상품 생성
            brands.forEach(brand -> {
                IntStream.range(0, 5).forEach(i -> {
                    ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);

                    productRepository.save(product);
                });
            });


            // when
            ProductDetailInfo productDetail = productFacade.getProductDetail(1L);

            // then
            assertThat(productDetail).isNotNull();

            assertThat(productDetail.name()).isNotNull();
            assertThat(productDetail.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(productDetail.brand()).isNotNull();
        }
    }
}
