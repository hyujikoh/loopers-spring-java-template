package com.loopers.domain.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandService;
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
    private BrandService brandService;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }



}
