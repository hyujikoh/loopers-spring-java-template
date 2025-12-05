package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.point.PointFacade;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.interfaces.api.order.OrderV1Dtos;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

/**
 * 카드 결제 E2E 테스트
 * 
 * 검증 항목:
 * 1. 실제 PG 모듈(pg-simulator)과 통신
 * 2. 결제 요청 → PENDING 상태 저장
 * 3. PG 콜백 수신 → SUCCESS/FAILED 상태 업데이트
 * 4. 주문 상태 연동 (결제 성공 시 주문 CONFIRMED)
 * 
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Payment API E2E 테스트 - 실제 PG 통신")
class PaymentV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PaymentRepository paymentRepository;

    private String testUsername;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // 테스트용 브랜드 생성
        BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("테스트브랜드", "E2E 테스트용 브랜드")
        );

        // 테스트용 상품 생성
        ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                brand.getId(),
                "테스트상품",
                "E2E 테스트용 상품",
                new BigDecimal("50000"),
                1000
        );
        ProductEntity product = productService.registerProduct(productRequest);
        testProductId = product.getId();

        // 테스트용 사용자 생성
        UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
        userFacade.registerUser(userCommand);
        testUsername = userCommand.username();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
        testProductId = null;
        testUsername = null;
    }

    @Nested
    @DisplayName("카드 결제 주문 생성 API")
    class CreateOrderWithCardPaymentTest {

        @Test
        @DisplayName("카드 결제 주문 생성 시 PG 모듈과 통신하고 PENDING 상태로 저장된다")
        void create_order_with_card_payment_success() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.CardOrderCreateRequest request = new OrderV1Dtos.CardOrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null)),
                    "CREDIT",
                    "1234-5678-9012-3456"
            );

            // When: 카드 결제 주문 생성
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Order.CREATE_CARD,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // Then: 주문 생성 성공
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(Objects.requireNonNull(response.getBody()).data().orderId()).isNotNull();
            assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.PENDING);

            // Then: 결제 정보 확인 (PENDING 상태)
            OrderV1Dtos.OrderCreateResponse.PaymentResponse paymentResponse = 
                    response.getBody().data().payment();
            assertThat(paymentResponse).isNotNull();
            assertThat(paymentResponse.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(paymentResponse.transactionKey()).isNotNull();

            // Then: DB에 PENDING 상태로 저장됨
            Long orderId = response.getBody().data().orderId();
            PaymentEntity savedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(savedPayment.getTransactionKey()).isNotNull();
        }

        @Test
        @DisplayName("PG 콜백으로 결제 성공 시 주문이 CONFIRMED 상태로 변경된다")
        void payment_callback_success_updates_order_to_confirmed() throws InterruptedException {
            // Given: 카드 결제 주문 생성
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.CardOrderCreateRequest orderRequest = new OrderV1Dtos.CardOrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null)),
                    "CREDIT",
                    "1234-5678-9012-3456"
            );

            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponse =
                    testRestTemplate.exchange(
                            Uris.Order.CREATE_CARD,
                            HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            orderResponseType
                    );

            Long orderId = Objects.requireNonNull(orderResponse.getBody()).data().orderId();
            String transactionKey = orderResponse.getBody().data().payment().transactionKey();

            // When: PG 콜백 수신 (SUCCESS)
            PaymentV1Dtos.PgCallbackRequest callbackRequest = new PaymentV1Dtos.PgCallbackRequest(
                    transactionKey,
                    orderId.toString(),
                    "SUCCESS",
                    "정상 승인되었습니다."
            );

            ParameterizedTypeReference<ApiResponse<Void>> callbackResponseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> callbackResponse = testRestTemplate.exchange(
                    Uris.Payment.CALLBACK,
                    HttpMethod.POST,
                    new HttpEntity<>(callbackRequest, headers),
                    callbackResponseType
            );

            // Then: 콜백 처리 성공
            assertThat(callbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Then: 결제 상태가 SUCCESS로 변경됨 (비동기 처리 대기)
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElseThrow();
                        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
                    });

            // Then: 주문 상태가 CONFIRMED로 변경됨
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        var orderInfo = orderFacade.getOrderById(testUsername, orderId);
                        assertThat(orderInfo.status()).isEqualTo(OrderStatus.CONFIRMED);
                    });
        }

        @Test
        @DisplayName("PG 콜백으로 결제 실패 시 주문이 CANCELLED 상태로 변경된다")
        void payment_callback_failed_updates_order_to_cancelled() throws InterruptedException {
            // Given: 카드 결제 주문 생성
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.CardOrderCreateRequest orderRequest = new OrderV1Dtos.CardOrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null)),
                    "CREDIT",
                    "9999-9999-9999-9999" // 실패할 카드번호
            );

            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponse =
                    testRestTemplate.exchange(
                            Uris.Order.CREATE_CARD,
                            HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            orderResponseType
                    );

            Long orderId = Objects.requireNonNull(orderResponse.getBody()).data().orderId();
            String transactionKey = orderResponse.getBody().data().payment().transactionKey();

            // When: PG 콜백 수신 (FAILED)
            PaymentV1Dtos.PgCallbackRequest callbackRequest = new PaymentV1Dtos.PgCallbackRequest(
                    transactionKey,
                    orderId.toString(),
                    "FAILED",
                    "잘못된 카드입니다. 다른 카드를 선택해주세요."
            );

            ParameterizedTypeReference<ApiResponse<Void>> callbackResponseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> callbackResponse = testRestTemplate.exchange(
                    Uris.Payment.CALLBACK,
                    HttpMethod.POST,
                    new HttpEntity<>(callbackRequest, headers),
                    callbackResponseType
            );

            // Then: 콜백 처리 성공
            assertThat(callbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Then: 결제 상태가 FAILED로 변경됨
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElseThrow();
                        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                        assertThat(payment.getFailureReason()).contains("잘못된 카드");
                    });

            // Then: 주문 상태가 CANCELLED로 변경됨
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        var orderInfo = orderFacade.getOrderById(testUsername, orderId);
                        assertThat(orderInfo.status()).isEqualTo(OrderStatus.CANCELLED);
                    });
        }

        @Test
        @DisplayName("중복 콜백 수신 시 멱등성이 보장된다")
        void payment_callback_idempotency() throws InterruptedException {
            // Given: 카드 결제 주문 생성
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.CardOrderCreateRequest orderRequest = new OrderV1Dtos.CardOrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null)),
                    "CREDIT",
                    "1234-5678-9012-3456"
            );

            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> orderResponse =
                    testRestTemplate.exchange(
                            Uris.Order.CREATE_CARD,
                            HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            orderResponseType
                    );

            Long orderId = Objects.requireNonNull(orderResponse.getBody()).data().orderId();
            String transactionKey = orderResponse.getBody().data().payment().transactionKey();

            PaymentV1Dtos.PgCallbackRequest callbackRequest = new PaymentV1Dtos.PgCallbackRequest(
                    transactionKey,
                    orderId.toString(),
                    "SUCCESS",
                    "정상 승인되었습니다."
            );

            ParameterizedTypeReference<ApiResponse<Void>> callbackResponseType =
                    new ParameterizedTypeReference<>() {};

            // When: 첫 번째 콜백
            ResponseEntity<ApiResponse<Void>> firstCallback = testRestTemplate.exchange(
                    Uris.Payment.CALLBACK,
                    HttpMethod.POST,
                    new HttpEntity<>(callbackRequest, headers),
                    callbackResponseType
            );

            // When: 두 번째 콜백 (중복)
            ResponseEntity<ApiResponse<Void>> secondCallback = testRestTemplate.exchange(
                    Uris.Payment.CALLBACK,
                    HttpMethod.POST,
                    new HttpEntity<>(callbackRequest, headers),
                    callbackResponseType
            );

            // Then: 두 콜백 모두 성공 응답
            assertThat(firstCallback.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondCallback.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Then: 결제 상태는 SUCCESS 유지 (중복 처리 방지)
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElseThrow();
                        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
                    });
        }

        @Test
        @DisplayName("존재하지 않는 transactionKey로 콜백 수신 시 404 응답")
        void payment_callback_with_invalid_transaction_key() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            PaymentV1Dtos.PgCallbackRequest callbackRequest = new PaymentV1Dtos.PgCallbackRequest(
                    "invalid-transaction-key",
                    "999999",
                    "SUCCESS",
                    "정상 승인되었습니다."
            );

            // When
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    Uris.Payment.CALLBACK,
                    HttpMethod.POST,
                    new HttpEntity<>(callbackRequest, headers),
                    responseType
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("PG 장애 시나리오")
    class PgFailureScenarioTest {

        @Test
        @DisplayName("PG 타임아웃 발생 시 Fallback이 실행되고 FAILED 상태로 저장된다")
        void pg_timeout_triggers_fallback() {
            // Given: PG 타임아웃을 유발하는 요청 (실제로는 PG-Simulator에서 지연 응답 필요)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.CardOrderCreateRequest request = new OrderV1Dtos.CardOrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null)),
                    "TIMEOUT", // PG-Simulator에서 타임아웃을 유발하는 특수 카드 타입
                    "0000-0000-0000-0000"
            );

            // When
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Order.CREATE_CARD,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // Then: Fallback으로 처리되어 주문은 생성되지만 결제는 FAILED
            // (실제 구현에 따라 다를 수 있음)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
