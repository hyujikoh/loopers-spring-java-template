package com.loopers.util;

import net.datafaker.Faker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandDomainCreateRequest;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 및 브랜드 데이터 생성 Runner
 * 애플리케이션 시작 시 자동으로 브랜드 및 상품 데이터를 생성합니다
 * 실행 방법:
 * --args='--spring.profiles.active=local --product.data.generate=true'
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.data.generate", havingValue = "true")
public class ProductDataGeneratorRunner implements CommandLineRunner {

    private final ProductJpaRepository productRepository;
    private final BrandJpaRepository brandRepository;
    private final Faker faker = new Faker(new Locale("ko"));
    private final Random random = new Random();

    private static final int BATCH_SIZE = 1000;
    private static final int BRAND_COUNT = 100;
    private static final int PRODUCTS_PER_BRAND = 20;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            log.info("데이터 생성 시작");

            // 1. 브랜드 데이터 생성
            List<BrandEntity> brands = generateAndSaveBrands();
            log.info("브랜드 생성 완료: {}개", brands.size());

            // 2. 상품 데이터 생성
            generateAndSaveProducts(brands);

            long duration = System.currentTimeMillis() - startTime;
            log.info("데이터 생성 완료 - 소요 시간: {}ms", duration);

        } catch (Exception e) {
            log.error("데이터 생성 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 브랜드 데이터를 생성하고 저장합니다.
     */
    private List<BrandEntity> generateAndSaveBrands() {
        List<BrandEntity> brands = new ArrayList<>();

        for (int i = 0; i < BRAND_COUNT; i++) {
            BrandEntity brand = BrandEntity.createBrandEntity(
                    new BrandDomainCreateRequest(
                            faker.company().name(),
                            faker.lorem().sentence()
                    )
            );
            brands.add(brand);

            // 배치 단위로 저장
            if (brands.size() % BATCH_SIZE == 0) {
                brandRepository.saveAll(brands);
                log.info("브랜드 배치 저장: {}개", brands.size());
                brands.clear();
            }
        }

        // 남은 데이터 저장
        if (!brands.isEmpty()) {
            brandRepository.saveAll(brands);
            log.info("브랜드 배치 저장: {}개", brands.size());
        }

        // 저장된 모든 브랜드 반환
        return brandRepository.findAll();
    }

    /**
     * 상품 데이터를 생성하고 저장합니다.
     */
    private void generateAndSaveProducts(List<BrandEntity> brands) {
        List<ProductEntity> products = new ArrayList<>();
        int totalProducts = 0;

        for (BrandEntity brand : brands) {
            for (int i = 0; i < PRODUCTS_PER_BRAND; i++) {
                ProductEntity product = createRandomProduct(brand.getId());
                products.add(product);

                // 배치 단위로 저장
                if (products.size() % BATCH_SIZE == 0) {
                    productRepository.saveAll(products);
                    totalProducts += products.size();
                    log.info("상품 배치 저장: {}개 (누적: {}개)", products.size(), totalProducts);
                    products.clear();
                }
            }
        }

        // 남은 데이터 저장
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
            totalProducts += products.size();
            log.info("상품 배치 저장: {}개 (누적: {}개)", products.size(), totalProducts);
        }

        log.info("상품 생성 완료: {}개", totalProducts);
    }

    /**
     * 무작위 상품을 생성합니다.
     */
    private ProductEntity createRandomProduct(Long brandId) {
        String productName = faker.commerce().productName();
        String description = faker.lorem().sentence();
        BigDecimal originPrice = generateRandomPrice(10000, 500000);
        BigDecimal discountPrice = random.nextDouble() > 0.5
                ? originPrice.multiply(BigDecimal.valueOf(random.nextDouble() * 0.3 + 0.7))
                .setScale(0, RoundingMode.HALF_UP)
                : null;
        int stockQuantity = random.nextInt(1000) + 1;

        return new ProductEntity(
                brandId,
                productName,
                description,
                originPrice,
                discountPrice,
                stockQuantity
        );
    }

    /**
     * 범위 내의 무작위 가격을 생성합니다.
     */
    private BigDecimal generateRandomPrice(int minPrice, int maxPrice) {
        return BigDecimal.valueOf(random.nextInt(maxPrice - minPrice + 1) + minPrice);
    }
}
