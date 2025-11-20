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
    }
}
