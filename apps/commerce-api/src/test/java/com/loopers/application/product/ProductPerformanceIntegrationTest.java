//package com.loopers.application.product;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import com.loopers.domain.brand.BrandRepository;
//import com.loopers.domain.product.ProductCacheService;
//import com.loopers.domain.product.ProductMVService;
//import com.loopers.domain.product.ProductRepository;
//import com.loopers.domain.product.dto.ProductSearchFilter;
//import com.loopers.fixtures.ProductTestFixture;
//import com.loopers.infrastructure.cache.CacheStrategy;
//import com.loopers.utils.DatabaseCleanUp;
//import com.loopers.utils.RedisCleanUp;
//
//import lombok.extern.slf4j.Slf4j;
//
/// **
// * 성능 테스트
// *
// * 상품 조회 성능, 캐시 히트율, 배치 업데이트 성능을 측정합니다.
// *
// * @author hyunjikoh
// * @since 2025. 11. 10.
// */
//@SpringBootTest
//@DisplayName("상품 조회 성능 테스트")
//@Slf4j
//public class ProductPerformanceIntegrationTest {
//
//    @Autowired
//    private DatabaseCleanUp databaseCleanUp;
//
//    @Autowired
//    private RedisCleanUp redisCleanUp;
//
//    @Autowired
//    private ProductFacade productFacade;
//
//    @Autowired
//    private ProductMVService mvService;
//
//    @Autowired
//    private ProductCacheService cacheService;
//
//    @Autowired
//    private BrandRepository brandRepository;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @AfterEach
//    void tearDown() {
//        databaseCleanUp.truncateAllTables();
//        redisCleanUp.truncateAll();
//    }
//
//    @Nested
//    @DisplayName("상품 목록 조회 응답 시간")
//    class ProductListResponseTimeTest {
//
//        @Test
//        @DisplayName("캐시 미스 시 상품 목록 조회 응답 시간이 50ms 이하다")
//        void should_respond_within_50ms_on_cache_miss() {
//            // Given: 상품 데이터 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 5, 20);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(0, 20);
//            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//
//            // When: 첫 조회 (캐시 미스)
//            Instant start = Instant.now();
//            Page<ProductInfo> result = productFacade.getProducts(filter);
//            Instant end = Instant.now();
//
//            long responseTimeMs = Duration.between(start, end).toMillis();
//
//            // Then: 응답 시간 검증
//            assertThat(result.getContent()).hasSize(20);
//            assertThat(responseTimeMs).isLessThanOrEqualTo(50L);
//        }
//
//        @Test
//        @DisplayName("캐시 히트 시 상품 목록 조회 응답 시간이 10ms 이하다")
//        void should_respond_within_10ms_on_cache_hit() {
//            // Given: 상품 데이터 생성 및 캐시 워밍업
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 5, 20);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(0, 20);
//            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//
//            // 캐시 워밍업
//            productFacade.getProducts(filter);
//
//            // When: 두 번째 조회 (캐시 히트)
//            Instant start = Instant.now();
//            Page<ProductInfo> result = productFacade.getProducts(filter);
//            Instant end = Instant.now();
//
//            long responseTimeMs = Duration.between(start, end).toMillis();
//
//            // Then: 응답 시간 검증
//            assertThat(result.getContent()).hasSize(20);
//            assertThat(responseTimeMs).isLessThanOrEqualTo(10L);
//        }
//
//        @Test
//        @DisplayName("브랜드별 상품 목록 조회 응답 시간이 50ms 이하다")
//        void should_respond_within_50ms_for_brand_products() {
//            // Given: 특정 브랜드의 상품 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 50);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(0, 20);
//            ProductSearchFilter filter = new ProductSearchFilter(1L, null, pageable);
//
//            // When: 브랜드별 조회
//            Instant start = Instant.now();
//            Page<ProductInfo> result = productFacade.getProducts(filter);
//            Instant end = Instant.now();
//
//            long responseTimeMs = Duration.between(start, end).toMillis();
//
//            // Then: 응답 시간 검증
//            assertThat(result.getContent()).hasSize(20);
//            assertThat(responseTimeMs).isLessThanOrEqualTo(50L);
//        }
//    }
//
//    @Nested
//    @DisplayName("상품 상세 조회 응답 시간")
//    class ProductDetailResponseTimeTest {
//
//        @Test
//        @DisplayName("캐시 미스 시 상품 상세 조회 응답 시간이 30ms 이하다")
//        void should_respond_within_30ms_on_cache_miss() {
//            // Given: 상품 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 10);
//            mvService.syncMaterializedView();
//            Long productId = 1L;
//
//            // When: 첫 조회 (캐시 미스)
//            Instant start = Instant.now();
//            ProductDetailInfo result = productFacade.getProductDetail(productId, null);
//            Instant end = Instant.now();
//
//            long responseTimeMs = Duration.between(start, end).toMillis();
//
//            // Then: 응답 시간 검증
//            assertThat(result).isNotNull();
//            assertThat(responseTimeMs).isLessThanOrEqualTo(30L);
//        }
//
//        @Test
//        @DisplayName("캐시 히트 시 상품 상세 조회 응답 시간이 5ms 이하다")
//        void should_respond_within_5ms_on_cache_hit() {
//            // Given: 상품 생성 및 캐시 워밍업
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 10);
//            mvService.syncMaterializedView();
//            Long productId = 1L;
//
//            // 캐시 워밍업
//            productFacade.getProductDetail(productId, null);
//
//            // When: 두 번째 조회 (캐시 히트)
//            Instant start = Instant.now();
//            ProductDetailInfo result = productFacade.getProductDetail(productId, null);
//            Instant end = Instant.now();
//
//            long responseTimeMs = Duration.between(start, end).toMillis();
//
//            // Then: 응답 시간 검증
//            assertThat(result).isNotNull();
//            assertThat(responseTimeMs).isLessThanOrEqualTo(5L);
//        }
//    }
//
//    @Nested
//    @DisplayName("캐시 히트율")
//    class CacheHitRateTest {
//
//        @Test
//        @DisplayName("Hot 데이터의 캐시 히트율이 95% 이상이다")
//        void should_achieve_95_percent_hit_rate_for_hot_data() {
//            // Given: 상품 데이터 생성 및 캐시 워밍업
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 50);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(0, 20);
//            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//
//            // 캐시 워밍업
//            productFacade.getProducts(filter);
//
//            // When: 100번 조회
//            int totalRequests = 100;
//            AtomicInteger cacheHits = new AtomicInteger(0);
//
//            for (int i = 0; i < totalRequests; i++) {
//                // 캐시 확인
//                if (cacheService.getProductIdsFromCache(CacheStrategy.HOT, null, pageable).isPresent()) {
//                    cacheHits.incrementAndGet();
//                }
//
//                // 조회
//                productFacade.getProducts(filter);
//            }
//
//            // Then: 히트율 검증
//            double hitRate = (double) cacheHits.get() / totalRequests * 100;
//            assertThat(hitRate).isGreaterThanOrEqualTo(95.0);
//        }
//
//        @Test
//        @DisplayName("Warm 데이터의 캐시 히트율이 80% 이상이다")
//        void should_achieve_80_percent_hit_rate_for_warm_data() {
//            // Given: 상품 데이터 생성 및 캐시 워밍업
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 100);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(2, 20); // 3페이지 (Warm)
//            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//
//            // 캐시 워밍업
//            productFacade.getProducts(filter);
//
//            // When: 100번 조회
//            int totalRequests = 100;
//            AtomicInteger cacheHits = new AtomicInteger(0);
//
//            for (int i = 0; i < totalRequests; i++) {
//                // 캐시 확인
//                if (cacheService.getProductIdsFromCache(CacheStrategy.WARM, null, pageable).isPresent()) {
//                    cacheHits.incrementAndGet();
//                }
//
//                // 조회
//                productFacade.getProducts(filter);
//            }
//
//            // Then: 히트율 검증
//            double hitRate = (double) cacheHits.get() / totalRequests * 100;
//            assertThat(hitRate).isGreaterThanOrEqualTo(80.0);
//        }
//
//        @Test
//        @DisplayName("여러 페이지를 랜덤 조회 시 전체 캐시 히트율이 70% 이상이다")
//        void should_achieve_70_percent_overall_hit_rate() {
//            // Given: 상품 데이터 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 150);
//            mvService.syncMaterializedView();
//
//            // 여러 페이지 캐시 워밍업 (Hot, Warm, Cold)
//            for (int page = 0; page < 10; page++) {
//                Pageable pageable = PageRequest.of(page, 20);
//                ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//                productFacade.getProducts(filter);
//            }
//
//            // When: 랜덤 페이지 100번 조회
//            int totalRequests = 100;
//            AtomicInteger cacheHits = new AtomicInteger(0);
//
//            for (int i = 0; i < totalRequests; i++) {
//                int randomPage = (int) (Math.random() * 10);
//                Pageable pageable = PageRequest.of(randomPage, 20);
//                ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//
//                // 캐시 확인
//                CacheStrategy strategy = determineCacheStrategy(randomPage);
//                if (cacheService.getProductIdsFromCache(strategy, null, pageable).isPresent()) {
//                    cacheHits.incrementAndGet();
//                }
//
//                // 조회
//                productFacade.getProducts(filter);
//            }
//
//            // Then: 전체 히트율 검증
//            double hitRate = (double) cacheHits.get() / totalRequests * 100;
//            assertThat(hitRate).isGreaterThanOrEqualTo(70.0);
//        }
//
//        private CacheStrategy determineCacheStrategy(int pageNumber) {
//            if (pageNumber < 2) {
//                return CacheStrategy.HOT;
//            } else if (pageNumber < 5) {
//                return CacheStrategy.WARM;
//            } else {
//                return CacheStrategy.COLD;
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("배치 업데이트 성능")
//    class BatchUpdatePerformanceTest {
//
//        @Test
//        @DisplayName("10,000건 배치 업데이트가 5초 이내에 완료된다")
//        void should_complete_batch_update_within_5_seconds_for_10000_records() {
//            // Given: 10,000개 상품 생성
//            int brandCount = 100;
//            int productsPerBrand = 100;
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, brandCount, productsPerBrand);
//
//            // When: 배치 업데이트 실행
//            Instant start = Instant.now();
//            BatchUpdateResult result = mvService.syncMaterializedView();
//            Instant end = Instant.now();
//
//            long durationMs = Duration.between(start, end).toMillis();
//
//            // Then: 소요 시간 검증
//            assertThat(result.getUpdatedCount()).isEqualTo(10000);
//            assertThat(durationMs).isLessThanOrEqualTo(5000L);
//        }
//
//        @Test
//        @DisplayName("1,000건 배치 업데이트가 500ms 이내에 완료된다")
//        void should_complete_batch_update_within_500ms_for_1000_records() {
//            // Given: 1,000개 상품 생성
//            int brandCount = 10;
//            int productsPerBrand = 100;
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, brandCount, productsPerBrand);
//
//            // When: 배치 업데이트 실행
//            Instant start = Instant.now();
//            BatchUpdateResult result = mvService.syncMaterializedView();
//            Instant end = Instant.now();
//
//            long durationMs = Duration.between(start, end).toMillis();
//
//            // Then: 소요 시간 검증
//            assertThat(result.getUpdatedCount()).isEqualTo(1000);
//            assertThat(durationMs).isLessThanOrEqualTo(500L);
//        }
//
//        @Test
//        @DisplayName("변경된 데이터만 업데이트하여 배치 성능이 향상된다")
//        void should_improve_performance_by_updating_only_changed_data() {
//            // Given: 1,000개 상품 생성 및 초기 동기화
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 10, 100);
//            mvService.syncMaterializedView();
//
//            // 일부 상품만 변경 (10개)
//            List<Long> productIds = new ArrayList<>();
//            for (long i = 1; i <= 10; i++) {
//                productIds.add(i);
//            }
//
//            productIds.forEach(id -> {
//                var product = productRepository.findById(id).orElseThrow();
//                var updatedProduct = new com.loopers.domain.product.ProductEntity(
//                    product.getBrandId(),
//                    "Updated Product " + id,
//                    product.getDescription(),
//                    product.getPrice().getOriginPrice(),
//                    product.getPrice().getDiscountPrice(),
//                    product.getStockQuantity()
//                );
//                productRepository.save(updatedProduct);
//            });
//
//            // When: 배치 재실행 (변경된 데이터만 업데이트)
//            Instant start = Instant.now();
//            BatchUpdateResult result = mvService.syncMaterializedView();
//            Instant end = Instant.now();
//
//            long durationMs = Duration.between(start, end).toMillis();
//
//            // Then: 변경된 데이터만 업데이트되고 빠르게 완료됨
//            assertThat(result.getUpdatedCount()).isLessThanOrEqualTo(10);
//            assertThat(durationMs).isLessThanOrEqualTo(100L);
//        }
//    }
//
//    @Nested
//    @DisplayName("동시 접속자 처리")
//    class ConcurrentRequestTest {
//
//        @Test
//        @DisplayName("100개 동시 요청을 처리할 수 있다")
//        void should_handle_100_concurrent_requests() throws InterruptedException {
//            // Given: 상품 데이터 생성 및 캐시 워밍업
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 50);
//            mvService.syncMaterializedView();
//
//            Pageable pageable = PageRequest.of(0, 20);
//            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
//            productFacade.getProducts(filter); // 캐시 워밍업
//
//            // When: 100개 동시 요청
//            int threadCount = 100;
//            List<Thread> threads = new ArrayList<>();
//            AtomicInteger successCount = new AtomicInteger(0);
//
//            for (int i = 0; i < threadCount; i++) {
//                Thread thread = new Thread(() -> {
//                    try {
//                        Page<ProductInfo> result = productFacade.getProducts(filter);
//                        if (result.getContent().size() == 20) {
//                            successCount.incrementAndGet();
//                        }
//                    } catch (Exception e) {
//                    }
//                });
//                threads.add(thread);
//                thread.start();
//            }
//
//            // 모든 스레드 완료 대기
//            for (Thread thread : threads) {
//                thread.join();
//            }
//
//            // Then: 모든 요청이 성공함
//            assertThat(successCount.get()).isEqualTo(threadCount);
//        }
//    }
//}
