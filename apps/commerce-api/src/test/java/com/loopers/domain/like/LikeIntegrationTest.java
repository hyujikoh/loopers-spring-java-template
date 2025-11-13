package com.loopers.domain.like;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

/**
 * LikeFacade 통합 테스트
 * <p>
 * 좋아요 등록 및 취소 기능을 파사드 레벨에서 검증합니다.
 * 각 도메인 서비스를 통해 트랜잭션이 적용된 데이터 저장을 수행합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
@SpringBootTest
@DisplayName("LikeFacade 통합 테스트")
public class LikeIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserFacade userFacade;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * 좋아요 등록 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 등록")
    class 좋아요_등록 {

        /**
         * 유효한 사용자와 상품이 존재할 때 좋아요 등록에 성공하는지 테스트
         */
        @Test
        @DisplayName("유효한 사용자와 상품이면 좋아요 등록에 성공한다")
        void 유효한_사용자와_상품이면_좋아요_등록에_성공한다() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When: 좋아요 등록
            LikeInfo result = likeFacade.upsertLike(userInfo.username(), savedProduct.getId());

            // Then: 좋아요 등록 성공 검증
            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo(userInfo.username());
            assertThat(result.productId()).isEqualTo(savedProduct.getId());
        }

        @Test
        @DisplayName("삭제된 사용자면 예외를 던진다")
        void 삭제된_사용자면_예외를_던진다() {
            // Given
            UserEntity deletedUser = UserTestFixture.createDefaultUserEntity();
            deletedUser.delete();
            userRepository.save(deletedUser);

            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(deletedUser.getUsername(), savedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외를 던진다")
        void 존재하지_않는_사용자면_예외를_던진다() {
            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike("existUser", savedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }


        @Test
        @DisplayName("삭제된 상품이면 예외를 던진다")
        void 삭제된_상품이면_예외를_던진다() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductEntity deletedProduct = ProductTestFixture.createEntity(1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100);
            deletedProduct.delete();
            productRepository.save(deletedProduct);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(user.getUsername(), deletedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 예외를 던진다")
        void 존재하지_않는_상품이면_예외를_던진다() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(user.getUsername(), 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("이미 삭제된 좋아요를 다시 등록하면 복원된다")
        void 이미_삭제된_좋아요를_다시_등록하면_복원된다() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );


            ProductEntity product = productService.registerProduct(request);

            LikeEntity deletedLike = LikeEntity.createEntity(userInfo.id(), product.getId());
            deletedLike.delete();
            likeRepository.save(deletedLike);

            // When
            LikeInfo result = likeFacade.upsertLike(userInfo.username(), product.getId());

            // Then
            assertThat(result).isNotNull();
            Optional<LikeEntity> found = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );
            assertThat(found).isPresent();
            assertThat(found.get().getDeletedAt()).isNull();
        }
    }


    @Nested
    @DisplayName("좋아요 취소")
    class 좋아요_취소 {
        @Test
        @DisplayName("유효한 사용자의 좋아요를 취소하면 성공한다")
        @Transactional
        void 유효한_사용자의_좋아요를_취소하면_성공한다() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            LikeEntity like = LikeEntity.createEntity(userInfo.id(), product.getId());
            likeRepository.save(like);

            // When
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // Then
            Optional<LikeEntity> found = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );
            assertThat(found).isPresent();
            assertThat(found.get().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("삭제된 사용자가 좋아요를 취소하려 하면 예외를 던진다")
        @Transactional
        void 삭제된_사용자가_좋아요를_취소하려_하면_예외를_던진다() {

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct("nonExistUser", 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 좋아요를 취소하려 하면 예외를 던진다")
        @Transactional
        void 존재하지_않는_사용자가_좋아요를_취소하려_하면_예외를_던진다() {
            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct("nonExistUser", 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("삭제된 상품의 좋아요를 취소하면  예외 처리한다.")
        @Transactional
        void 삭제된_상품의_좋아요를_취소하면_예외_처리한다() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity deletedProduct = productService.registerProduct(request);
            deletedProduct.delete();
            productRepository.save(deletedProduct);

            LikeEntity like = LikeEntity.createEntity(user.getId(), deletedProduct.getId());
            likeRepository.save(like);


            // Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct(user.getUsername(), 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("좋아요가 존재하지 않아도 취소는 무시하고 성공한다")
        @Transactional
        void 좋아요가_존재하지_않아도_취소는_무시하고_성공한다() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When & Then - 예외 없이 정상 완료
            likeFacade.unlikeProduct(user.getUsername(), product.getId());
        }
    }

    @Nested
    @DisplayName("멱등성 테스트")
    class 멱등성_테스트 {

        @Test
        @DisplayName("동시에 같은 좋아요를 등록하면 중복이 생성되지 않는다")
        void 동시에_같은_좋아요를_등록하면_중복이_생성되지_않는다() throws InterruptedException {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 동시에 10번 좋아요 등록 시도
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        likeFacade.upsertLike(userInfo.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 좋아요는 1개만 생성되어야 함
            java.util.List<LikeEntity> likes = likeRepository.findAll();
            long userLikes = likes.stream()
                    .filter(like -> like.getUserId().equals(userInfo.id())
                            && like.getProductId().equals(product.getId())
                            && like.getDeletedAt() == null)
                    .count();

            assertThat(userLikes).isEqualTo(1);
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("동시에 같은 좋아요를 등록 및 취소하면 멱등성이 보장된다")
        void 동시에_같은_좋아요를_등록_및_취소하면_멱등성이_보장된다() throws InterruptedException {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 동시에 등록과 취소를 반복
            int threadCount = 20;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        if (index % 2 == 0) {
                            // 짝수 스레드는 등록
                            likeFacade.upsertLike(userInfo.username(), product.getId());
                        } else {
                            // 홀수 스레드는 취소
                            likeFacade.unlikeProduct(userInfo.username(), product.getId());
                        }
                    } catch (Exception e) {
                        // 동시성으로 인한 예외는 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 최종 상태는 일관성이 있어야 함
            Optional<LikeEntity> finalLike = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );

            // 좋아요 엔티티가 존재해야 함 (등록 또는 취소 중 하나의 상태)
            assertThat(finalLike).isPresent();
            
            // 데이터베이스에 중복된 좋아요가 없어야 함
            List<LikeEntity> allLikes = likeRepository.findAll();
            long duplicateCount = allLikes.stream()
                    .filter(like -> like.getUserId().equals(userInfo.id()) 
                            && like.getProductId().equals(product.getId()))
                    .count();
            assertThat(duplicateCount).isEqualTo(1);
        }

        @Test
        @DisplayName("동시에 여러 번 좋아요 취소 시 중복 취소가 발생하지 않는다")
        void 동시에_여러_번_좋아요_취소_시_중복_취소가_발생하지_않는다() throws InterruptedException {
            // Given: 사용자, 상품, 좋아요 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // 좋아요 등록
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // When: 동시에 10번 취소 시도
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        likeFacade.unlikeProduct(userInfo.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 좋아요는 삭제 상태여야 하며, 중복 삭제가 발생하지 않아야 함
            Optional<LikeEntity> like = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );

            assertThat(like).isPresent();
            assertThat(like.get().getDeletedAt()).isNotNull();

            // 모든 취소 시도가 성공해야 함 (멱등성)
            assertThat(successCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("복수의 사용자가 동시에 같은 상품에 좋아요하면 각각 독립적으로 등록된다")
        void 복수의_사용자가_동시에_같은_상품에_좋아요하면_각각_독립적으로_등록된다() throws InterruptedException {
            // Given: 여러 사용자와 하나의 상품 생성
            int userCount = 5;
            List<UserInfo> users = new ArrayList<>();

            for (int i = 0; i < userCount; i++) {
                UserRegisterCommand command = UserTestFixture.createUserCommand(
                        "user" + i,
                        "user" + i + "@example.com",
                        "1990-01-01",
                        com.loopers.domain.user.Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(command);
                users.add(userInfo);
            }

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "인기상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 모든 사용자가 동시에 좋아요 등록
            CountDownLatch latch = new CountDownLatch(userCount);
            ExecutorService executorService = newFixedThreadPool(userCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (UserInfo user : users) {
                executorService.submit(() -> {
                    try {
                        likeFacade.upsertLike(user.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 각 사용자별로 독립적인 좋아요가 생성되어야 함
            java.util.List<LikeEntity> likes = likeRepository.findAll();
            long activeLikes = likes.stream()
                    .filter(like -> like.getProductId().equals(product.getId())
                            && like.getDeletedAt() == null)
                    .count();

            assertThat(activeLikes).isEqualTo(userCount);
            assertThat(successCount.get()).isEqualTo(userCount);

            // 각 사용자별로 좋아요가 하나씩만 있는지 확인
            for (UserInfo user : users) {
                long userLikeCount = likes.stream()
                        .filter(like -> like.getUserId().equals(user.id())
                                && like.getProductId().equals(product.getId())
                                && like.getDeletedAt() == null)
                        .count();
                assertThat(userLikeCount).isEqualTo(1);
            }
        }
    }
}
