package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import java.math.BigDecimal;
import java.util.List;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order 도메인 통합 테스트
 * 
 * <p>주문 생성, 확정, 취소 등의 전체 플로우를 검증합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@SpringBootTest
@DisplayName("Order 도메인 통합 테스트")
public class OrderIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserService userService;


    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("주문 생성")
    class OrderCreation {

        @Test
        @DisplayName("유효한 사용자와 상품으로 주문을 생성하면 성공한다")
        void should_create_order_successfully_with_valid_user_and_products() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명"));

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // Given: 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(savedProduct.getId())
                                    .quantity(2)
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문이 정상 생성되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userInfo.id());
            assertThat(result.totalAmount()).isEqualTo(new BigDecimal("20000.00")); // 10000 * 2
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.orderItems()).hasSize(1);
            assertThat(result.orderItems().get(0).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("주문 생성 시 주문 항목들이 함께 저장된다")
        void should_save_order_items_when_creating_order() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 여러 상품 생성
            ProductEntity product1 = productService.registerProduct(
                ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품1",
                    "상품1 설명",
                    new BigDecimal("10000"),
                    100
                )
            );
            
            ProductEntity product2 = productService.registerProduct(
                ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품2",
                    "상품2 설명",
                    new BigDecimal("20000"),
                    50
                )
            );
            
            ProductEntity product3 = productService.registerProduct(
                ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품3",
                    "상품3 설명",
                    new BigDecimal("15000"),
                    30
                )
            );

            // Given: 여러 상품을 포함한 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                .username(userInfo.username())
                .orderItems(List.of(
                    OrderItemCommand.builder()
                        .productId(product1.getId())
                        .quantity(2)
                        .build(),
                    OrderItemCommand.builder()
                        .productId(product2.getId())
                        .quantity(1)
                        .build(),
                    OrderItemCommand.builder()
                        .productId(product3.getId())
                        .quantity(3)
                        .build()
                ))
                .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문 항목들이 모두 저장되었는지 검증
            assertThat(result.orderItems()).isNotNull();
            assertThat(result.orderItems()).hasSize(3);
            
            // Then: 각 주문 항목의 정보가 정확한지 검증
            assertThat(result.orderItems())
                .extracting("productId", "quantity", "unitPrice")
                .containsExactlyInAnyOrder(
                    tuple(product1.getId(), 2, new BigDecimal("10000.00")),
                    tuple(product2.getId(), 1, new BigDecimal("20000.00")),
                    tuple(product3.getId(), 3, new BigDecimal("15000.00"))
                );
            
            // Then: 각 주문 항목의 총액이 정확한지 검증
            assertThat(result.orderItems().get(0).totalPrice())
                .isEqualTo(result.orderItems().get(0).unitPrice()
                    .multiply(BigDecimal.valueOf(result.orderItems().get(0).quantity())));
            
            // Then: 주문 총액이 모든 항목의 합계와 일치하는지 검증
            BigDecimal expectedTotal = new BigDecimal("10000.00").multiply(BigDecimal.valueOf(2))  // 20000
                .add(new BigDecimal("20000.00").multiply(BigDecimal.valueOf(1)))                    // 20000
                .add(new BigDecimal("15000.00").multiply(BigDecimal.valueOf(3)));                   // 45000
            assertThat(result.totalAmount()).isEqualTo(expectedTotal);  // 85000.00
        }

        @Test
        @Transactional
        @DisplayName("주문 생성 시 총액이 주문 항목들의 합계와 일치한다")
        void should_match_total_amount_with_sum_of_order_items() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("재고가 부족한 상품으로 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_insufficient_stock() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("포인트가 부족한 경우 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_insufficient_points() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("삭제된 상품으로 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_deleted_product() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("주문 확정")
    class OrderConfirmation {

        @Test
        @Transactional
        @DisplayName("PENDING 상태의 주문을 확정하면 CONFIRMED 상태로 변경된다")
        void should_change_status_to_confirmed_when_confirming_pending_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 확정 시 상품 재고가 차감된다")
        void should_decrease_product_stock_when_confirming_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("PENDING이 아닌 상태의 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_non_pending_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("존재하지 않는 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_non_existent_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("삭제된 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_deleted_order() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class OrderCancellation {

        @Test
        @Transactional
        @DisplayName("PENDING 상태의 주문을 취소하면 CANCELLED 상태로 변경된다")
        void should_change_status_to_cancelled_when_cancelling_pending_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("CONFIRMED 상태의 주문을 취소하면 CANCELLED 상태로 변경된다")
        void should_change_status_to_cancelled_when_cancelling_confirmed_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 취소 시 상품 재고가 복원된다")
        void should_restore_product_stock_when_cancelling_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("이미 취소된 주문을 다시 취소하면 예외가 발생한다")
        void should_throw_exception_when_cancelling_already_cancelled_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 예외가 발생한다")
        void should_throw_exception_when_cancelling_non_existent_order() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("주문 조회")
    class OrderRetrieval {

        @Test
        @Transactional
        @DisplayName("주문 ID로 주문을 조회할 수 있다")
        void should_retrieve_order_by_id() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("사용자 ID로 주문 목록을 조회할 수 있다")
        void should_retrieve_orders_by_user_id() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 조회 시 주문 항목들이 함께 조회된다")
        void should_retrieve_order_with_order_items() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        void should_throw_exception_when_retrieving_non_existent_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("삭제된 주문은 조회되지 않는다")
        void should_not_retrieve_deleted_orders() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("사용자의 주문 목록을 페이징하여 조회할 수 있다")
        void should_retrieve_user_orders_with_pagination() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("주문 상태별로 필터링하여 조회할 수 있다")
        void should_filter_orders_by_status() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("주문 항목 관리")
    class OrderItemManagement {

        @Test
        @Transactional
        @DisplayName("주문에 여러 개의 주문 항목을 추가할 수 있다")
        void should_add_multiple_order_items_to_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 항목의 총액이 정확히 계산된다")
        void should_calculate_order_item_total_correctly() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 항목들의 총액 합계가 주문 총액과 일치한다")
        void should_match_order_total_with_sum_of_item_totals() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("주문 항목이 없는 주문은 생성할 수 없다")
        void should_not_create_order_without_order_items() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("트랜잭션 및 동시성")
    class TransactionAndConcurrency {

        @Test
        @DisplayName("재고 예약 실패 시 이미 예약된 재고가 해제된다")
        void should_release_reserved_stock_when_subsequent_reservation_fails() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("포인트 차감 실패 시 예약된 모든 재고가 해제된다")
        void should_release_all_reserved_stock_when_point_deduction_fails() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("주문 생성 실패 시 포인트 복구 및 재고 해제가 수행된다")
        void should_restore_points_and_release_stock_when_order_creation_fails() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @DisplayName("동시에 같은 상품을 주문하면 재고가 정확히 차감된다")
        void should_decrease_stock_correctly_when_concurrent_orders_for_same_product() {
            // TODO: TDD - 구현 필요
        }
    }

    @Nested
    @DisplayName("주문 총액 계산")
    class OrderTotalCalculation {

        @Test
        @Transactional
        @DisplayName("단일 항목 주문의 총액이 정확히 계산된다")
        void should_calculate_total_correctly_for_single_item_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("다중 항목 주문의 총액이 정확히 계산된다")
        void should_calculate_total_correctly_for_multiple_items_order() {
            // TODO: TDD - 구현 필요
        }

        @Test
        @Transactional
        @DisplayName("수량이 많은 주문의 총액이 정확히 계산된다")
        void should_calculate_total_correctly_for_large_quantity_order() {
            // TODO: TDD - 구현 필요
        }
    }
}
