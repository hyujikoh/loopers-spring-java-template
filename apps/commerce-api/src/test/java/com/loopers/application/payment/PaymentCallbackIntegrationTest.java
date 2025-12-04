package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

/**
 * Level 2: 통합 테스트 - 결제 콜백 처리
 *
 * P0 우선순위 테스트:
 * - PG-Simulator의 실제 콜백 구조 기반 테스트
 * - 결제 성공 콜백 (SUCCESS) → 주문 확정
 * - 결제 실패 콜백 (FAILED) → 주문 취소
 * - 멱등성 검증 (중복 콜백)
 *
 * PG-Simulator 콜백 데이터 구조:
 * - transactionKey: 트랜잭션 키
 * - orderId: 주문 ID
 * - cardType: 카드 타입
 * - cardNo: 마스킹된 카드 번호
 * - amount: 결제 금액 (Long)
 * - status: SUCCESS, FAILED, PENDING
 * - reason: 실패 사유 (nullable)
 *
 * 검증 항목:
 * - PaymentEntity 상태 전이
 * - OrderEntity 상태 전이
 * - 이벤트 기반 비동기 처리
 * - 데이터 일관성
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@SpringBootTest
@DisplayName("결제 콜백 처리 통합 테스트 (PG-Simulator 기준)")
class PaymentCallbackIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderFacade orderFacade;

    @MockitoBean
    private PgClient pgClient;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("결제 성공 콜백 처리 (SUCCESS)")
    class 결제_성공_콜백_처리 {

        @Test
        @DisplayName("SUCCESS 콜백 수신 시 결제 완료 처리되고 주문이 확정된다")
        void SUCCESS_콜백_수신_시_결제_완료_처리되고_주문이_확정된다() {
            // Given: 사용자, 주문, PENDING 상태 결제 준비
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // PG-Simulator가 보내는 실제 콜백 구조
            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);

            // When: SUCCESS 콜백 처리
            paymentFacade.handlePaymentCallback(successCallback);

            // Then: 결제 상태 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(updatedPayment.getCompletedAt()).isNotNull();

            // Then: 비동기 이벤트 처리 대기 (주문 확정)
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    OrderEntity updatedOrder = orderRepository.findByIdAndUserId(order.getId(), user.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                });
        }

        @Test
        @DisplayName("SUCCESS 콜백 수신 후 이벤트가 정상 발행된다")
        void SUCCESS_콜백_수신_후_이벤트가_정상_발행된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);

            // When
            paymentFacade.handlePaymentCallback(successCallback);

            // Then: 결제 완료 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Then: 주문 확정 확인 (이벤트 처리 결과)
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    OrderEntity updatedOrder = orderRepository.findByIdAndUserId(order.getId(), user.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                });
        }
    }

    @Nested
    @DisplayName("결제 실패 콜백 처리 (FAILED)")
    class 결제_실패_콜백_처리 {

        @Test
        @DisplayName("FAILED 콜백 수신 시 결제 실패 처리되고 주문이 취소된다")
        void FAILED_콜백_수신_시_결제_실패_처리되고_주문이_취소된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            String failureReason = "잔액 부족";
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, failureReason);

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 결제 실패 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(updatedPayment.getFailureReason()).isEqualTo(failureReason);

            // Then: 주문 취소 확인
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    OrderEntity updatedOrder = orderRepository.findByIdAndUserId(order.getId(), user.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
        }

        @Test
        @DisplayName("FAILED 콜백에 실패 사유가 포함되어 저장된다")
        void FAILED_콜백에_실패_사유가_포함되어_저장된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            String failureReason = "카드 한도 초과";
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, failureReason);

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then
            PaymentEntity updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(updatedPayment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("FAILED 콜백 수신 후 주문 취소 이벤트가 발행된다")
        void FAILED_콜백_수신_후_주문_취소_이벤트가_발행된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, "유효하지 않은 카드");

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 비동기 이벤트 처리 대기
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    OrderEntity updatedOrder = orderRepository.findByIdAndUserId(order.getId(), user.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
        }
    }

    @Nested
    @DisplayName("멱등성 검증")
    class 멱등성_검증 {

        @Test
        @DisplayName("이미 COMPLETED 상태인 결제에 SUCCESS 콜백이 다시 오면 처리를 무시한다")
        void 이미_COMPLETED_상태인_결제에_SUCCESS_콜백이_다시_오면_처리를_무시한다() {
            // Given: COMPLETED 상태 결제
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // 첫 번째 콜백으로 COMPLETED 상태로 만듦
            PaymentV1Dtos.PgCallbackRequest firstCallback = createSuccessCallback(payment, order);
            paymentFacade.handlePaymentCallback(firstCallback);

            // 상태 확인
            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    PaymentEntity completedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
                    assertThat(completedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
                });

            PaymentEntity completedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            var originalCompletedAt = completedPayment.getCompletedAt();

            // When: 두 번째 SUCCESS 콜백 (중복)
            PaymentV1Dtos.PgCallbackRequest duplicateCallback = createSuccessCallback(payment, order);
            paymentFacade.handlePaymentCallback(duplicateCallback);

            // Then: 상태 변경 없음
            PaymentEntity unchangedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(unchangedPayment.getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("이미 FAILED 상태인 결제에 FAILED 콜백이 다시 오면 처리를 무시한다")
        void 이미_FAILED_상태인_결제에_FAILED_콜백이_다시_오면_처리를_무시한다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // 첫 번째 실패 콜백
            String firstReason = "첫 번째 실패";
            PaymentV1Dtos.PgCallbackRequest firstCallback = createFailedCallback(payment, order, firstReason);
            paymentFacade.handlePaymentCallback(firstCallback);

            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    PaymentEntity failedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
                    assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                });

            PaymentEntity failedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            String originalFailureReason = failedPayment.getFailureReason();

            // When: 두 번째 실패 콜백 (다른 사유)
            String secondReason = "두 번째 실패";
            PaymentV1Dtos.PgCallbackRequest duplicateCallback = createFailedCallback(payment, order, secondReason);
            paymentFacade.handlePaymentCallback(duplicateCallback);

            // Then: failureReason 변경 없음 (첫 번째 값 유지)
            PaymentEntity unchangedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(unchangedPayment.getFailureReason()).isEqualTo(originalFailureReason);
        }

        @Test
        @DisplayName("COMPLETED 상태에 FAILED 콜백이 오면 처리를 무시한다")
        void COMPLETED_상태에_FAILED_콜백이_오면_처리를_무시한다() {
            // Given: COMPLETED 상태 결제
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);
            paymentFacade.handlePaymentCallback(successCallback);

            await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    PaymentEntity completedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
                    assertThat(completedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
                });

            // When: FAILED 콜백 시도
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, "뒤늦은 실패");
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 여전히 COMPLETED 상태 유지
            PaymentEntity unchangedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(unchangedPayment.getFailureReason()).isNull();
        }
    }

    @Nested
    @DisplayName("PENDING 상태 콜백 처리")
    class PENDING_상태_콜백_처리 {

        @Test
        @DisplayName("PENDING 콜백 수신 시 로그만 출력하고 상태 변경하지 않는다")
        void PENDING_콜백_수신_시_로그만_출력하고_상태_변경하지_않는다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest pendingCallback = new PaymentV1Dtos.PgCallbackRequest(
                payment.getTransactionKey(),
                order.getId().toString(),
                "CREDIT",
                "1234-****-****-3456",
                50000L,
                "PENDING",
                null
            );

            // When
            paymentFacade.handlePaymentCallback(pendingCallback);

            // Then: PENDING 상태 유지
            PaymentEntity unchangedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("알 수 없는 상태 처리")
    class 알_수_없는_상태_처리 {

        @Test
        @DisplayName("알 수 없는 상태 콜백은 로그만 남기고 상태 변경하지 않는다")
        void 알_수_없는_상태_콜백은_로그만_남기고_상태_변경하지_않는다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest unknownCallback = new PaymentV1Dtos.PgCallbackRequest(
                payment.getTransactionKey(),
                order.getId().toString(),
                "CREDIT",
                "1234-****-****-3456",
                50000L,
                "UNKNOWN_STATUS",
                null
            );

            // When
            paymentFacade.handlePaymentCallback(unknownCallback);

            // Then: PENDING 상태 유지
            PaymentEntity unchangedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    // ==================== 테스트 헬퍼 메서드 ====================

    private UserEntity createAndSaveUser() {
        UserEntity user = UserTestFixture.createDefaultUserEntity();
        return userRepository.save(user);
    }

    private OrderEntity createAndSaveOrder(Long userId) {
        OrderDomainCreateRequest request = new OrderDomainCreateRequest(
            userId,
            new BigDecimal("50000.00"),
            BigDecimal.ZERO,
            new BigDecimal("50000.00")
        );
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }

    private PaymentEntity createAndSavePendingPayment(UserEntity user, OrderEntity order) {
        PaymentCommand command = PaymentCommand.builder()
            .username(user.getUsername())
            .orderId(order.getId())
            .cardType("CREDIT")
            .cardNo("1234-5678-9012-3456")
            .amount(order.getFinalTotalAmount())
            .callbackUrl("http://localhost:8080/api/v1/payments/callback")
            .build();

        PaymentEntity payment = PaymentEntity.createPending(user, command);
        payment.updateTransactionKey("TXN_TEST_" + System.currentTimeMillis());
        return paymentRepository.save(payment);
    }

    /**
     * PG-Simulator의 SUCCESS 콜백 생성
     */
    private PaymentV1Dtos.PgCallbackRequest createSuccessCallback(PaymentEntity payment, OrderEntity order) {
        return new PaymentV1Dtos.PgCallbackRequest(
            payment.getTransactionKey(),
            order.getId().toString(),
            "CREDIT",
            "1234-****-****-3456",
            order.getFinalTotalAmount().longValue(),
            "SUCCESS",
            null
        );
    }

    /**
     * PG-Simulator의 FAILED 콜백 생성
     */
    private PaymentV1Dtos.PgCallbackRequest createFailedCallback(PaymentEntity payment, OrderEntity order, String reason) {
        return new PaymentV1Dtos.PgCallbackRequest(
            payment.getTransactionKey(),
            order.getId().toString(),
            "CREDIT",
            "1234-****-****-3456",
            order.getFinalTotalAmount().longValue(),
            "FAILED",
            reason
        );
    }
}

