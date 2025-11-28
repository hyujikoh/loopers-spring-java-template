package com.loopers.util;

import net.datafaker.Faker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductEntity;
import com.loopers.infrastructure.product.ProductJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 데이터 생성 Runner
 * 애플리케이션 시작 시 자동으로 상품 데이터를 생성합니다
 * 실행 방법:
 * --args='--spring.profiles.active=local --product.data.generate=true'
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.data.generate", havingValue = "true")
public class ProductDataGeneratorRunner implements CommandLineRunner {

    private final ProductJpaRepository productRepository;
    private final Faker faker = new Faker(new Locale("ko"));
    private final Random random = new Random();

    private static final int BATCH_SIZE = 1000;
    private static final int BRAND_COUNT = 100;

    @Override
    public void run(String... args) {
        log.info("=== 상품 데이터 자동 생성 시작 ===");

        // 이미 데이터가 있으면 생성하지 않음
        long existingCount = productRepository.count();
        if (existingCount > 0) {
            log.info("이미 {}개의 상품 데이터가 존재합니다. 생성을 건너뜁니다.", existingCount);
            return;
        }

        int totalCount = 100_000; // 기본값

        log.info("생성 개수: {}개", totalCount);
        log.info("배치 크기: {}개", BATCH_SIZE);

        long startTime = System.currentTimeMillis();

        generateProducts(totalCount);

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        log.info("=== 상품 데이터 생성 완료 ===");
        log.info("총 소요 시간: {}초 ({}분)", duration, duration / 60);
        log.info("총 생성 개수: {}개", productRepository.count());
    }

    public void generateProducts(int totalCount) {
        int batchCount = totalCount / BATCH_SIZE;

        for (int i = 0; i < batchCount; i++) {
            List<ProductEntity> products = new ArrayList<>(BATCH_SIZE);

            for (int j = 0; j < BATCH_SIZE; j++) {
                products.add(createRandomProduct());
            }

            productRepository.saveAll(products);

            if ((i + 1) % 10 == 0) {
                log.info("진행 상황: {}/{} 배치 완료 ({}개 생성, {}%)",
                        i + 1, batchCount, (i + 1) * BATCH_SIZE,
                        String.format("%.1f", ((i + 1) * 100.0 / batchCount)));
            }
        }
    }

    private ProductEntity createRandomProduct() {
        Long brandId = (long) ThreadLocalRandom.current().nextInt(1, BRAND_COUNT + 1);
        String name = generateProductName();
        String description = generateDescription();
        BigDecimal originPrice = generateOriginPrice();
        BigDecimal discountPrice = generateDiscountPrice(originPrice);
        Integer stockQuantity = generateStockQuantity();

        ProductEntity product = new ProductEntity(
                brandId,
                name,
                description,
                originPrice,
                discountPrice,
                stockQuantity
        );

        // 좋아요 수는 MV 테이블에서 관리하므로 여기서는 설정하지 않음
        // 필요시 ProductLikeStatsEntity를 별도로 생성

        return product;
    }

    private String generateProductName() {
        String[] prefixes = {
                "프리미엄", "베스트", "신상", "인기", "특가",
                "한정판", "시즌", "트렌디", "클래식", "모던"
        };

        String[] categories = {
                "티셔츠", "셔츠", "바지", "청바지", "원피스",
                "자켓", "코트", "니트", "후드", "맨투맨"
        };

        String prefix = prefixes[random.nextInt(prefixes.length)];
        String category = categories[random.nextInt(categories.length)];
        String detail = faker.commerce().productName();

        return String.format("%s %s - %s", prefix, category, detail);
    }

    private String generateDescription() {
        if (random.nextInt(10) < 3) {
            return null;
        }

        return faker.lorem().sentence(10) + " " + faker.lorem().sentence(15);
    }

    private BigDecimal generateOriginPrice() {
        int price = ThreadLocalRandom.current().nextInt(10, 501) * 1000;
        return new BigDecimal(price);
    }

    private BigDecimal generateDiscountPrice(BigDecimal originPrice) {
        if (random.nextBoolean()) {
            return null;
        }

        double discountRate = 0.1 + (random.nextDouble() * 0.4);
        BigDecimal discountPrice = originPrice.multiply(BigDecimal.valueOf(1 - discountRate));

        return discountPrice.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Integer generateStockQuantity() {
        int[] stockOptions = {0, 5, 10, 20, 50, 100, 200, 500, 1000};
        return stockOptions[random.nextInt(stockOptions.length)];
    }
}
