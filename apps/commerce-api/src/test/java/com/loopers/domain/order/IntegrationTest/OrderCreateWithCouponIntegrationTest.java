package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

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
import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * 쿠폰을 사용한 주문 생성 통합 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 20.
 */
@SpringBootTest
@DisplayName("쿠폰을 사용한 주문 생성 통합 테스트")
public class OrderCreateWithCouponIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("정액 할인 쿠폰 주문")
    class FixedAmountCouponOrder {

        @Test
        @DisplayName("정액 할인 쿠폰을 사용하여 주문을 생성할 수 있다")
        void should_create_order_with_fixed_amount_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명"));

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);
            pointService.charge(userInfo.username(), new BigDecimal("15000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            UserEntity userByUsername = userService.getUserByUsername(userInfo.username());

            CouponEntity fixedAmountCoupon = couponService.createFixedAmountCoupon(userByUsername, new BigDecimal("5000"));

            // Given: 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(savedProduct.getId())
                                    .quantity(2)
                                    .couponId(fixedAmountCoupon.getId())
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);


            // Then
            CouponEntity usedCoupon = couponService.getCouponById(fixedAmountCoupon.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        }

        @Test
        @DisplayName("정액 쿠폰 적용 후 총 주문 금액이 정확하게 계산된다")
        void should_calculate_total_amount_correctly_with_fixed_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성 (단가 10,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 정액 할인 쿠폰 생성 (5,000원 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity fixedCoupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );

            // Given: 주문 생성 요청 (수량 2개)
            // 예상 계산: (10,000 * 2) - 5,000 = 15,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .couponId(fixedCoupon.getId())
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 총 주문 금액 검증
            BigDecimal expectedOriginalAmount = new BigDecimal("20000"); // 10,000 * 2
            BigDecimal expectedDiscountAmount = new BigDecimal("5000");  // 쿠폰 할인
            BigDecimal expectedFinalAmount = new BigDecimal("15000");    // 20,000 - 5,000

            assertThat(result.totalAmount())
                    .as("쿠폰 할인이 적용된 최종 주문 금액")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 주문 항목 금액 검증
            assertThat(result.orderItems()).hasSize(1);
            assertThat(result.orderItems().get(0).quantity()).isEqualTo(2);
            assertThat(result.orderItems().get(0).unitPrice())
                    .as("상품 단가")
                    .isEqualByComparingTo(new BigDecimal("10000"));

            // Then: 실제 할인이 적용되었는지 검증
            BigDecimal itemTotal = result.orderItems().get(0).unitPrice()
                    .multiply(BigDecimal.valueOf(result.orderItems().get(0).quantity()));
            BigDecimal actualDiscount = itemTotal.subtract(result.totalAmount());
            
            assertThat(actualDiscount)
                    .as("실제 적용된 할인 금액")
                    .isEqualByComparingTo(expectedDiscountAmount);
        }


    }
}
