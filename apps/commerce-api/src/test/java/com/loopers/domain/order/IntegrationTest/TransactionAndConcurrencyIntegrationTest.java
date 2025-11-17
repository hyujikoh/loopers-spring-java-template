package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 17.
 */
@SpringBootTest
public class TransactionAndConcurrencyIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;
    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("트랜잭션 및 동시성")
    class TransactionAndConcurrency {

        @Test
        @DisplayName("재고 예약 실패 시 이미 예약된 재고가 해제된다")
        void should_release_reserved_stock_when_subsequent_reservation_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 두 개의 상품 생성 (하나는 재고 부족)
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "정상상품",
                            "재고가 충분한 상품",
                            new BigDecimal("10000"),
                            100
                    )
            );

            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "재고부족상품",
                            "재고가 부족한 상품",
                            new BigDecimal("20000"),
                            5  // 재고 5개
                    )
            );

            // Given: 첫 번째 상품은 정상, 두 번째 상품은 재고 부족으로 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product1.getId())
                                    .quantity(10)  // 정상 주문
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product2.getId())
                                    .quantity(10)  // 재고 부족 (5개만 있음)
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 상품입니다.");

            // Then: 첫 번째 상품의 재고가 원래대로 복구되었는지 확인
            ProductEntity product1AfterFail = productService.getProductDetail(product1.getId());
            assertThat(product1AfterFail.getStockQuantity()).isEqualTo(100);

            // Then: 두 번째 상품의 재고도 변경되지 않았는지 확인
            ProductEntity product2AfterFail = productService.getProductDetail(product2.getId());
            assertThat(product2AfterFail.getStockQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("포인트 차감 실패 시 예약된 모든 재고가 해제된다")
        void should_release_all_reserved_stock_when_point_deduction_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 부족한 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("5000"));  // 부족한 포인트

            // Given: 여러 상품 생성
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품1",
                            "설명1",
                            new BigDecimal("10000"),
                            100
                    )
            );

            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품2",
                            "설명2",
                            new BigDecimal("20000"),
                            50
                    )
            );

            ProductEntity product3 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품3",
                            "설명3",
                            new BigDecimal("15000"),
                            30
                    )
            );

            // Given: 포인트가 부족한 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(1).build(),
                            OrderItemCommand.builder().productId(product3.getId()).quantity(3).build()
                    ))
                    .build();

            // When & Then: 포인트 부족으로 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            // Then: 모든 상품의 재고가 원래대로 복구되었는지 확인
            ProductEntity product1AfterFail = productService.getProductDetail(product1.getId());
            assertThat(product1AfterFail.getStockQuantity()).isEqualTo(100);

            ProductEntity product2AfterFail = productService.getProductDetail(product2.getId());
            assertThat(product2AfterFail.getStockQuantity()).isEqualTo(50);

            ProductEntity product3AfterFail = productService.getProductDetail(product3.getId());
            assertThat(product3AfterFail.getStockQuantity()).isEqualTo(30);

            // Then: 사용자 포인트도 차감되지 않았는지 확인
            UserEntity user = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(user.getPointAmount()).isEqualTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("주문 생성 실패 시 포인트 복구 및 재고 해제가 수행된다")
        void should_restore_points_and_release_stock_when_order_creation_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("100000.00");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "테스트상품",
                            "상품 설명",
                            new BigDecimal("10000"),
                            100
                    )
            );

            Integer initialStock = product.getStockQuantity();

            // Given: 유효하지 않은 주문 생성 요청 (수량이 0)
            OrderCreateCommand invalidOrderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(0)  // 유효하지 않은 수량
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(invalidOrderCommand))
                    .isInstanceOf(Exception.class);

            // Then: 재고가 원래대로 복구되었는지 확인
            ProductEntity productAfterFail = productService.getProductDetail(product.getId());
            assertThat(productAfterFail.getStockQuantity()).isEqualTo(initialStock);

            // Then: 포인트가 차감되지 않았는지 확인
            UserEntity user = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(user.getPointAmount()).isEqualTo(initialPoints);
        }

        @Test
        @DisplayName("동시에 같은 상품을 주문할 때 재고 차감이 정확하게 발생한다")
        void should_deduct_stock_accurately_when_ordering_same_product_concurrently() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 여러 사용자 생성 및 포인트 충전
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("50000"));
                users.add(userInfo);
            }

            // Given: 재고가 충분한 상품 생성
            Integer initialStock = 50;
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "인기상품",
                            "동시성 테스트용 상품",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 동시 주문 설정
            int threadCount = 10;
            int orderQuantityPerUser = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // When: 10명의 사용자가 동시에 각각 5개씩 주문 시도
            for (int i = 0; i < threadCount; i++) {
                final int userIndex = i;
                executorService.submit(() -> {
                    try {
                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(users.get(userIndex).username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(orderQuantityPerUser)
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then: 모든 스레드 완료 대기
            latch.await(30, TimeUnit.SECONDS);
            executorService.shutdown();

            // Then: 재고가 충분하므로 모든 주문이 성공해야 함
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(failCount.get()).isZero();

            // Then: 최종 재고가 정확히 차감되었는지 검증 (동시성 제어 완벽)
            ProductEntity finalProduct = productService.getProductDetail(product.getId());
            int expectedFinalStock = initialStock - (threadCount * orderQuantityPerUser);

            // ✅ 엄격한 검증: 정확히 일치해야 함
            assertThat(finalProduct.getStockQuantity())
                    .as("비관적 락으로 동시성이 보호되므로 재고는 정확히 일치해야 함")
                    .isEqualTo(expectedFinalStock);

            // ✅ 추가 검증: Oversell 절대 발생하지 않음
            assertThat(finalProduct.getStockQuantity())
                    .as("재고는 절대 음수가 될 수 없음")
                    .isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("동시에 같은 상품을 주문할 때 재고 부족 시 일부만 성공한다")
        void should_succeed_partially_when_stock_insufficient_during_concurrent_orders() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 여러 사용자 생성 및 포인트 충전
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("100000"));
                users.add(userInfo);
            }

            // Given: 재고가 부족한 상품 생성
            Integer initialStock = 25;  // 10명이 5개씩 주문하면 부족 (50개 필요)
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "한정상품",
                            "재고가 부족한 한정 상품",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 동시 주문 설정
            int threadCount = 10;
            int orderQuantityPerUser = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // When: 10명의 사용자가 동시에 각각 5개씩 주문 시도
            for (int i = 0; i < threadCount; i++) {
                final int userIndex = i;
                executorService.submit(() -> {
                    try {
                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(users.get(userIndex).username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(orderQuantityPerUser)
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then: 모든 스레드 완료 대기
            latch.await(30, TimeUnit.SECONDS);
            executorService.shutdown();

            // Then: 일부 주문만 성공했는지 확인 (재고 부족으로 모두 성공할 수 없음)
            assertThat(successCount.get()).isLessThan(threadCount);
            assertThat(failCount.get()).isGreaterThan(0);

            // Then: 성공 + 실패 = 전체 시도 수
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

            // Then: 최종 재고 확인 (재고가 소진되었거나 거의 소진됨)
            ProductEntity finalProduct = productService.getProductDetail(product.getId());
            assertThat(finalProduct.getStockQuantity()).isLessThanOrEqualTo(initialStock);

            // Then: 최소한 일부 재고는 차감되었는지 확인
            assertThat(finalProduct.getStockQuantity()).isLessThan(initialStock);
        }

        @Test
        @DisplayName("주문 취소 시 트랜잭션이 롤백되면 재고와 포인트가 원상태로 유지된다")
        void should_maintain_stock_and_points_when_cancel_transaction_rollback() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("50000.00");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성
            Integer initialStock = 100;
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "테스트상품",
                            "상품 설명",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 주문 생성
            Integer orderQuantity = 5;
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(orderQuantity)
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 생성 후 상태 확인
            ProductEntity productAfterOrder = productService.getProductDetail(product.getId());
            assertThat(productAfterOrder.getStockQuantity()).isEqualTo(initialStock - orderQuantity);

            UserEntity userAfterOrder = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            BigDecimal pointsAfterOrder = userAfterOrder.getPointAmount();
            assertThat(pointsAfterOrder).isLessThan(initialPoints);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 재고 원복 확인
            ProductEntity productAfterCancel = productService.getProductDetail(product.getId());
            assertThat(productAfterCancel.getStockQuantity()).isEqualTo(initialStock);

            // Then: 포인트 환불 확인
            UserEntity userAfterCancel = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(userAfterCancel.getPointAmount()).isEqualTo(initialPoints);

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
