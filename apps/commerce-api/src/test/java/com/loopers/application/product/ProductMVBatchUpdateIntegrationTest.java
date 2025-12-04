//package com.loopers.application.product;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.math.BigDecimal;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import com.loopers.application.like.LikeFacade;
//import com.loopers.application.user.UserFacade;
//import com.loopers.application.user.UserInfo;
//import com.loopers.application.user.UserRegisterCommand;
//import com.loopers.domain.brand.BrandEntity;
//import com.loopers.domain.brand.BrandRepository;
//import com.loopers.domain.like.LikeService;
//import com.loopers.domain.product.*;
//import com.loopers.fixtures.BrandTestFixture;
//import com.loopers.fixtures.ProductTestFixture;
//import com.loopers.fixtures.UserTestFixture;
//import com.loopers.utils.DatabaseCleanUp;
//import com.loopers.utils.RedisCleanUp;
//
/// **
// * 배치 업데이트 통합 테스트
// *
// * MV 테이블 배치 동기화 및 데이터 일관성을 검증합니다.
// *
// * @author hyunjikoh
// * @since 2025. 11. 10.
// */
//@SpringBootTest
//@DisplayName("배치 업데이트 통합 테스트")
//public class ProductMVBatchUpdateIntegrationTest {
//
//    @Autowired
//    private DatabaseCleanUp databaseCleanUp;
//
//    @Autowired
//    private RedisCleanUp redisCleanUp;
//
//    @Autowired
//    private ProductMVService mvService;
//
//    @Autowired
//    private BrandRepository brandRepository;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private ProductMVRepository mvRepository;
//
//    @Autowired
//    private UserFacade userFacade;
//
//    @Autowired
//    private LikeFacade likeFacade;
//
//    @Autowired
//    private LikeService likeService;
//
//    @AfterEach
//    void tearDown() {
//        databaseCleanUp.truncateAllTables();
//        redisCleanUp.truncateAll();
//    }
//
//    @Nested
//    @DisplayName("상품 정보 변경 후 배치 동기화")
//    class ProductChangeSyncTest {
//
//        @Test
//        @DisplayName("상품 이름 변경 후 배치 실행 시 MV가 동기화된다")
//        void should_sync_mv_when_product_name_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Original Name",
//                "Description",
//                new BigDecimal("10000"),
//                100
//            );
//
//            BatchUpdateResult initialSync = mvService.syncMaterializedView();
//            assertThat(initialSync.getUpdatedCount()).isGreaterThan(0);
//
//            // When: 상품 정보 변경
//            // TODO: ProductEntity에 update 메서드 추가 필요
//            // 현재는 테스트 스킵 (실제 구현 시 수정 필요)
//            // product.updateName("Updated Name");
//            // productRepository.save(product);
//
//            // When: 배치 재실행
//            BatchUpdateResult updateSync = mvService.syncMaterializedView();
//
//            // Then: MV가 갱신됨
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvEntity.getName()).isEqualTo("Updated Name");
//            assertThat(updateSync.getUpdatedCount()).isGreaterThan(0);
//        }
//
//        @Test
//        @DisplayName("상품 가격 변경 후 배치 실행 시 MV가 동기화된다")
//        void should_sync_mv_when_product_price_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Test Product",
//                "Description",
//                new BigDecimal("10000"),
//                100
//            );
//
//            mvService.syncMaterializedView();
//
//            // When: 상품 가격 변경 (새 상품으로 대체)
//            ProductEntity updatedProduct = new ProductEntity(
//                product.getBrandId(),
//                product.getName(),
//                product.getDescription(),
//                new BigDecimal("15000"),
//                null,
//                product.getStockQuantity()
//            );
//            productRepository.save(updatedProduct);
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV가 갱신됨
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvEntity.getPrice().getOriginPrice()).isEqualByComparingTo(new BigDecimal("15000"));
//        }
//
//        @Test
//        @DisplayName("상품 재고 변경 후 배치 실행 시 MV가 동기화된다")
//        void should_sync_mv_when_product_stock_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Test Product",
//                "Description",
//                new BigDecimal("10000"),
//                100
//            );
//
//            mvService.syncMaterializedView();
//
//            // When: 상품 재고 변경
//            product.deductStock(50);
//            productRepository.save(product);
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV가 갱신됨
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvEntity.getStockQuantity()).isEqualTo(50);
//        }
//    }
//
//    @Nested
//    @DisplayName("브랜드 정보 변경 후 배치 동기화")
//    class BrandChangeSyncTest {
//
//        @Test
//        @DisplayName("브랜드 이름 변경 후 배치 실행 시 해당 브랜드의 모든 상품 MV가 동기화된다")
//        void should_sync_all_products_mv_when_brand_name_changes() {
//            // Given: 브랜드와 여러 상품 생성
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Original Brand", "Description");
//            ProductEntity product1 = ProductTestFixture.createAndSave(productRepository, brand);
//            ProductEntity product2 = ProductTestFixture.createAndSave(productRepository, brand);
//            ProductEntity product3 = ProductTestFixture.createAndSave(productRepository, brand);
//
//            mvService.syncMaterializedView();
//
//            // When: 브랜드 이름 변경 (새 브랜드로 대체)
//            BrandEntity updatedBrand = new BrandEntity("Updated Brand", brand.getDescription());
//            brandRepository.save(updatedBrand);
//
//            // When: 배치 재실행
//            BatchUpdateResult result = mvService.syncMaterializedView();
//
//            // Then: 모든 상품의 MV가 갱신됨
//            ProductMaterializedViewEntity mv1 = mvRepository.findById(product1.getId()).orElseThrow();
//            ProductMaterializedViewEntity mv2 = mvRepository.findById(product2.getId()).orElseThrow();
//            ProductMaterializedViewEntity mv3 = mvRepository.findById(product3.getId()).orElseThrow();
//
//            assertThat(mv1.getBrandName()).isEqualTo("Updated Brand");
//            assertThat(mv2.getBrandName()).isEqualTo("Updated Brand");
//            assertThat(mv3.getBrandName()).isEqualTo("Updated Brand");
//            assertThat(result.getUpdatedCount()).isEqualTo(3);
//        }
//
//        @Test
//        @DisplayName("브랜드 설명 변경 후 배치 실행 시 MV가 동기화된다")
//        void should_sync_mv_when_brand_description_changes() {
//            // Given: 브랜드와 상품 생성
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Original Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            mvService.syncMaterializedView();
//
//            // When: 브랜드 설명 변경 (새 브랜드로 대체)
//            BrandEntity updatedBrand = new BrandEntity(brand.getName(), "Updated Description");
//            brandRepository.save(updatedBrand);
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV가 갱신됨 (브랜드 이름은 동일)
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvEntity.getBrandName()).isEqualTo("Test Brand");
//        }
//    }
//
//    @Nested
//    @DisplayName("좋아요 변경 후 배치 동기화")
//    class LikeChangeSyncTest {
//
//        @Test
//        @DisplayName("좋아요 추가 후 배치 실행 시 MV의 좋아요 수가 증가한다")
//        void should_increase_like_count_in_mv_when_like_added() {
//            // Given: 사용자와 상품 생성
//            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
//            UserInfo user = userFacade.registerUser(userCommand);
//
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            mvService.syncMaterializedView();
//
//            // 초기 좋아요 수 확인
//            ProductMaterializedViewEntity mvBefore = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvBefore.getLikeCount()).isEqualTo(0L);
//
//            // When: 좋아요 추가
//            likeFacade.upsertLike(user.username(), product.getId());
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV의 좋아요 수가 증가함
//            ProductMaterializedViewEntity mvAfter = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvAfter.getLikeCount()).isEqualTo(1L);
//        }
//
//        @Test
//        @DisplayName("좋아요 취소 후 배치 실행 시 MV의 좋아요 수가 감소한다")
//        void should_decrease_like_count_in_mv_when_like_removed() {
//            // Given: 사용자와 상품 생성, 좋아요 추가
//            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
//            UserInfo user = userFacade.registerUser(userCommand);
//
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            likeFacade.upsertLike(user.username(), product.getId());
//            mvService.syncMaterializedView();
//
//            // 좋아요 수 확인
//            ProductMaterializedViewEntity mvBefore = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvBefore.getLikeCount()).isEqualTo(1L);
//
//            // When: 좋아요 취소
//            likeFacade.unlikeProduct(user.username(), product.getId());
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV의 좋아요 수가 감소함
//            ProductMaterializedViewEntity mvAfter = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvAfter.getLikeCount()).isEqualTo(0L);
//        }
//
//        @Test
//        @DisplayName("여러 사용자의 좋아요 후 배치 실행 시 MV의 좋아요 수가 정확히 집계된다")
//        void should_aggregate_like_count_correctly_when_multiple_users_like() {
//            // Given: 여러 사용자 생성
//            UserInfo user1 = userFacade.registerUser(UserTestFixture.createUserCommand(
//                "user1", "user1@test.com", "1990-01-01", com.loopers.domain.user.Gender.MALE
//            ));
//            UserInfo user2 = userFacade.registerUser(UserTestFixture.createUserCommand(
//                "user2", "user2@test.com", "1990-01-01", com.loopers.domain.user.Gender.FEMALE
//            ));
//            UserInfo user3 = userFacade.registerUser(UserTestFixture.createUserCommand(
//                "user3", "user3@test.com", "1990-01-01", com.loopers.domain.user.Gender.MALE
//            ));
//
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            mvService.syncMaterializedView();
//
//            // When: 여러 사용자가 좋아요 추가
//            likeFacade.upsertLike(user1.username(), product.getId());
//            likeFacade.upsertLike(user2.username(), product.getId());
//            likeFacade.upsertLike(user3.username(), product.getId());
//
//            // When: 배치 재실행
//            mvService.syncMaterializedView();
//
//            // Then: MV의 좋아요 수가 정확히 집계됨
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvEntity.getLikeCount()).isEqualTo(3L);
//        }
//    }
//
//    @Nested
//    @DisplayName("데이터 일관성 검증")
//    class DataConsistencyTest {
//
//        @Test
//        @DisplayName("배치 후 MV의 좋아요 수는 Like 테이블의 실제 집계와 일치한다")
//        void should_match_like_count_between_mv_and_like_table() {
//            // Given: 여러 상품과 좋아요 생성
//            UserInfo user1 = userFacade.registerUser(UserTestFixture.createUserCommand(
//                "user1", "user1@test.com", "1990-01-01", com.loopers.domain.user.Gender.MALE
//            ));
//            UserInfo user2 = userFacade.registerUser(UserTestFixture.createUserCommand(
//                "user2", "user2@test.com", "1990-01-01", com.loopers.domain.user.Gender.FEMALE
//            ));
//
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product1 = ProductTestFixture.createAndSave(productRepository, brand);
//            ProductEntity product2 = ProductTestFixture.createAndSave(productRepository, brand);
//
//            // 상품1: 2개 좋아요
//            likeFacade.upsertLike(user1.username(), product1.getId());
//            likeFacade.upsertLike(user2.username(), product1.getId());
//
//            // 상품2: 1개 좋아요
//            likeFacade.upsertLike(user1.username(), product2.getId());
//
//            // When: 배치 실행
//            mvService.syncMaterializedView();
//
//            // Then: MV의 좋아요 수와 실제 집계가 일치함
//            ProductMaterializedViewEntity mv1 = mvRepository.findById(product1.getId()).orElseThrow();
//            ProductMaterializedViewEntity mv2 = mvRepository.findById(product2.getId()).orElseThrow();
//
//            Long actualLikeCount1 = likeService.countByProduct(product1);
//            Long actualLikeCount2 = likeService.countByProduct(product2);
//
//            assertThat(mv1.getLikeCount()).isEqualTo(actualLikeCount1).isEqualTo(2L);
//            assertThat(mv2.getLikeCount()).isEqualTo(actualLikeCount2).isEqualTo(1L);
//        }
//
//        @Test
//        @DisplayName("배치 후 MV의 상품 정보는 Product 테이블과 동기화된다")
//        void should_match_product_info_between_mv_and_product_table() {
//            // Given: 상품 생성 및 배치 실행
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Test Product",
//                "Test Description",
//                new BigDecimal("10000"),
//                100
//            );
//
//            mvService.syncMaterializedView();
//
//            // Then: MV와 Product 테이블의 정보가 일치함
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//
//            assertThat(mvEntity.getProductId()).isEqualTo(product.getId());
//            assertThat(mvEntity.getName()).isEqualTo(product.getName());
//            assertThat(mvEntity.getDescription()).isEqualTo(product.getDescription());
//            assertThat(mvEntity.getPrice().getOriginPrice()).isEqualByComparingTo(product.getPrice().getOriginPrice());
//            assertThat(mvEntity.getStockQuantity()).isEqualTo(product.getStockQuantity());
//        }
//
//        @Test
//        @DisplayName("배치 후 MV의 브랜드 정보는 Brand 테이블과 동기화된다")
//        void should_match_brand_info_between_mv_and_brand_table() {
//            // Given: 브랜드와 상품 생성 및 배치 실행
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            mvService.syncMaterializedView();
//
//            // Then: MV와 Brand 테이블의 정보가 일치함
//            ProductMaterializedViewEntity mvEntity = mvRepository.findById(product.getId()).orElseThrow();
//
//            assertThat(mvEntity.getBrandId()).isEqualTo(brand.getId());
//            assertThat(mvEntity.getBrandName()).isEqualTo(brand.getName());
//        }
//    }
//
//    @Nested
//    @DisplayName("배치 업데이트 결과")
//    class BatchUpdateResultTest {
//
//        @Test
//        @DisplayName("배치 실행 시 갱신된 레코드 수가 정확히 기록된다")
//        void should_record_updated_count_correctly() {
//            // Given: 여러 상품 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5);
//
//            // When: 배치 실행
//            BatchUpdateResult result = mvService.syncMaterializedView();
//
//            // Then: 갱신 건수가 기록됨
//            assertThat(result.getUpdatedCount()).isEqualTo(10); // 2개 브랜드 * 5개 상품
//            assertThat(result.isSuccess()).isTrue();
//        }
//
//        @Test
//        @DisplayName("배치 실행 시 소요 시간이 기록된다")
//        void should_record_duration_correctly() {
//            // Given: 상품 생성
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 10);
//
//            // When: 배치 실행
//            BatchUpdateResult result = mvService.syncMaterializedView();
//
//            // Then: 소요 시간이 기록됨
//            assertThat(result.getDurationMs()).isGreaterThan(0L);
//        }
//
//        @Test
//        @DisplayName("변경사항이 없으면 hasChanges가 false다")
//        void should_return_false_when_no_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5);
//            mvService.syncMaterializedView();
//
//            // When: 변경 없이 배치 재실행
//            BatchUpdateResult result = mvService.syncMaterializedView();
//
//            // Then: 변경사항 없음
//            assertThat(result.hasChanges()).isFalse();
//            assertThat(result.getUpdatedCount()).isEqualTo(0);
//        }
//
//        @Test
//        @DisplayName("변경사항이 있으면 hasChanges가 true다")
//        void should_return_true_when_has_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//            mvService.syncMaterializedView();
//
//            // When: 상품 변경 후 배치 재실행
//            ProductEntity updatedProduct = new ProductEntity(
//                product.getBrandId(),
//                "Updated Name",
//                product.getDescription(),
//                product.getPrice().getOriginPrice(),
//                product.getPrice().getDiscountPrice(),
//                product.getStockQuantity()
//            );
//            productRepository.save(updatedProduct);
//
//            BatchUpdateResult result = mvService.syncMaterializedView();
//
//            // Then: 변경사항 있음
//            assertThat(result.hasChanges()).isTrue();
//            assertThat(result.getUpdatedCount()).isGreaterThan(0);
//        }
//    }
//}
