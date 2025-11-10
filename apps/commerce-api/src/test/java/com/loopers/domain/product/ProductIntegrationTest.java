package com.loopers.domain.product;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandService;
import com.loopers.fixtures.BrandTestFixture;
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
            BrandEntity andSave = BrandTestFixture.createAndSave(brandRepository, "Nike", "jump man");
            ProductDomainRequest productDomainRequest = ProductDomainRequest.withoutDiscount(
                    andSave, "Air Max", "Comfortable running shoes", new BigDecimal("100000"), 50
            );


            // when
            ProductEntity save = productRepository.save(ProductEntity.createEntity(productDomainRequest));

            Assertions.assertThat(save).isNotNull();
            Assertions.assertThat(save.getId()).isNotNull();
            Assertions.assertThat(save.getName()).isEqualTo("Air Max");
            Assertions.assertThat(save.getBrand().getId()).isEqualTo(andSave.getId());
        }
    }
}
