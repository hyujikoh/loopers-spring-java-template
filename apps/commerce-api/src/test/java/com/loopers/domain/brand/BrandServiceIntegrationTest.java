package com.loopers.domain.brand;


import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.fixtures.BrandTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 8.
 */
@SpringBootTest
@DisplayName("BrandService 통합 테스트")
public class BrandServiceIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BrandService brandService;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("페이지네이션으로 브랜드 목록을 조회하는데 성공한다.")
    void list_brands_with_pagination_success() {
        // given
        BrandTestFixture.saveBrands(brandRepository, 5);

        Pageable pageable = Pageable.ofSize(3).withPage(0);
        Page<BrandEntity> resultPages = brandService.listBrands(pageable);

        // then
        assertThat(resultPages.getContent()).hasSize(3);
        assertThat(resultPages.getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("브랜드 상세 정보를 조회하는데 성공한다.")
    void get_brand_detail_success() {
    	// given
        BrandTestFixture.saveBrands(brandRepository, 5);

        // when
        BrandEntity brand = brandService.getBrandById(1L);

        assertThat(brand).isNotNull();
        Assertions.assertThat(brand.getId()).isEqualTo(1L);
        Assertions.assertThat(brand.getName()).isEqualTo(brand.getName());
        Assertions.assertThat(brand.getCreatedAt()).isEqualTo(brand.getCreatedAt());
        Assertions.assertThat(brand.getDeletedAt()).isNull();

    }

    @Test
    @DisplayName("브랜드의 id 기준 찾지 못하면 null을 반환한다")
    void get_brand_return_null_when_not_found() {
        // given
        BrandTestFixture.saveBrands(brandRepository, 5);

        // when
        BrandEntity brand = brandService.getBrandById(6L);

        assertThat(brand).isNull();
    }
}
