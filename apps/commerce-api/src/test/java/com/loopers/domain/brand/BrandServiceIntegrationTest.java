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
    @DisplayName("페이지네이션으로 브랜드 목록을 조회한다")
    void list_brands_with_pagination() {
        // given
        BrandTestFixture.saveBrands(brandRepository, 5);

        Pageable pageable = Pageable.ofSize(3).withPage(0);
        Page<BrandEntity> resultPages = brandService.listBrands(pageable);

        // then
        assertThat(resultPages.getContent()).hasSize(3);
        assertThat(resultPages.getTotalElements()).isEqualTo(5L);
    }
}
