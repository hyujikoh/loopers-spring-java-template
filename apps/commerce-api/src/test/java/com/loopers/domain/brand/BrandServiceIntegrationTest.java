package com.loopers.domain.brand;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @org.junit.jupiter.api.Test
    void 브랜드_목록_조회_브랜드_리스트를페이지네이션하게응답한다() {
    	// given
        BrandEntity brand = BrandEntity.createBrandEntity("Test Brand", "This is a test brand.");
        BrandEntity brand = BrandEntity.createBrandEntity("Test Brand", "This is a test brand.");
        BrandEntity brand = BrandEntity.createBrandEntity("Test Brand", "This is a test brand.");
        BrandEntity brand = BrandEntity.createBrandEntity("Test Brand", "This is a test brand.");
        BrandEntity brand = BrandEntity.createBrandEntity("Test Brand", "This is a test brand.");

        brandRepository.save(brand);


    }
}
