package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import net.datafaker.Faker;

/**
 * Level 3: Resilience4j Circuit Breaker 동작 검증 테스트
 *
 * 멘토링 핵심 강조 사항:
 * "서킷 브레이커 구현의 성패는 코드 작성이 아니라,
 *  의도한 대로 동작하는지를 어떻게 증명하는가에 달려있습니다."
 *
 * 검증 항목:
 * 1. minimumNumberOfCalls 로직 정확성
 * 2. failureRateThreshold 계산 정확성
 * 3. Circuit Breaker 상태 전환 (CLOSED → OPEN → HALF_OPEN → CLOSED)
 * 4. OPEN 상태에서 호출 차단
 * 5. Fallback 로직 실행
 *
 * 현재 설정 (application.yml):
 * - sliding-window-size: 10
 * - minimum-number-of-calls: 5
 * - failure-rate-threshold: 50
 * - wait-duration-in-open-state: 60s
 * - permitted-number-of-calls-in-half-open-state: 3
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@SpringBootTest
@DisplayName("Level 3: Circuit Breaker 동작 검증 테스트")
class CircuitBreakerIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PgClient pgClient;

    private CircuitBreaker circuitBreaker;


    private static final Faker faker = new Faker(new Locale("en"));


    @BeforeEach
    void setUp() {
        // Circuit Breaker 초기화
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgClient");
        circuitBreaker.reset(); // 상태 리셋
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
        circuitBreaker.reset();
    }

    @Nested
    @DisplayName("1. minimumNumberOfCalls 검증")
    class MinimumNumberOfCalls_검증 {

        @Test
        @DisplayName("minimumNumberOfCalls(5) 미만에서는 실패율과 관계없이 Circuit이 CLOSED 유지")
        void minimumNumberOfCalls_미만에서는_Circuit이_CLOSED_유지() {
            // Given: PG 호출 시 항상 실패하도록 설정
            given(pgClient.requestPayment(anyString(), any()))
                .willThrow(createPgException());

            // When: 4회 연속 실패 (minimumNumberOfCalls = 5 미만)
            // Retry가 각 3회씩 재시도하므로 실제 PG 호출은 12회
            for (int i = 0; i < 4; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                // Fallback이 실행되어 예외 없이 정상 반환
                PaymentInfo result = paymentFacade.processPayment(command);

                // Fallback 결과 검증: FAILED 상태, transactionKey 없음
                assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
                assertThat(result.transactionKey()).isNull();
            }

            // Then: Circuit 상태 = CLOSED (아직 최소 호출 수 미만)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // Metrics 확인
            // Fallback이 성공적으로 실행되었으므로 Circuit Breaker는 성공으로 기록
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);  // Fallback 성공 4번
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(4);
            assertThat(metrics.getFailureRate()).isEqualTo(-1.0f);  // minimumNumberOfCalls 미만이므로 -1
        }

        @Test
        @DisplayName("minimumNumberOfCalls(5) 이상이고 실패율 50% 이하면 Circuit이 CLOSED 유지")
        void 실패율_50퍼센트_이하면_Circuit이_CLOSED_유지() {
            // Given: 성공/실패 번갈아 발생
            given(pgClient.requestPayment(anyString(), any()))
                .willReturn(createSuccessResponse())  // 1회 성공
                .willReturn(createSuccessResponse())  // 2회 성공
                .willReturn(createSuccessResponse())  // 3회 성공
                .willThrow(createPgException())       // 4회 실패
                .willThrow(createPgException());      // 5회 실패

            // When: 5회 호출 (성공 3, 실패 2)
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = CLOSED (실패율 40% < 50%)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(3);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
            assertThat(metrics.getFailureRate()).isEqualTo(40.0f); // 2/5 = 40%
        }
    }

    @Nested
    @DisplayName("2. failureRateThreshold 검증")
    class FailureRateThreshold_검증 {

        @Test
        @DisplayName("5회 호출 중 3회 실패 시 실패율 60%로 Circuit이 OPEN으로 전환")
        void 실패율_60퍼센트_초과_시_Circuit이_OPEN_전환() {
            // Given
            given(pgClient.requestPayment(anyString(), any()))
                .willReturn(createSuccessResponse())  // 1회 성공
                .willReturn(createSuccessResponse())  // 2회 성공
                .willThrow(createPgException())       // 3회 실패
                .willThrow(createPgException())       // 4회 실패
                .willThrow(createPgException());      // 5회 실패 → 실패율 60%

            // When: 5회 호출
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = OPEN
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
            assertThat(metrics.getFailureRate())
                .isGreaterThanOrEqualTo(50.0f)  // 60% > 50%
                .isEqualTo(60.0f);
        }

        @Test
        @DisplayName("10회 호출 과정에서 8번째 호출에서 50% 를 도달했기 떄문에 서킷 이 열린다." )
        void slidingWindow_10개_중_6개_실패_시_Circuit이_OPEN_전환() {
            // Given
            given(pgClient.requestPayment(anyString(), any()))
                .willReturn(createSuccessResponse())  // 1~4회 성공
                .willReturn(createSuccessResponse())
                .willReturn(createSuccessResponse())
                .willReturn(createSuccessResponse())
                .willThrow(createPgException())       // 5~10회 실패 (6회)
                .willThrow(createPgException())
                .willThrow(createPgException())
                .willThrow(createPgException())
                .willThrow(createPgException())
                .willThrow(createPgException());

            // When: 10회 호출
            for (int i = 0; i < 10; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = OPEN (6/10 = 60% > 50%)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getFailureRate()).isEqualTo(50.0f);
        }
    }

    @Nested
    @DisplayName("3. OPEN 상태에서 호출 차단")
    class OPEN_상태에서_호출_차단 {

        @Test
        @DisplayName("Circuit이 OPEN 상태일 때 추가 호출은 차단되고 Fallback 실행")
        void Circuit이_OPEN_상태일_때_추가_호출은_차단되고_Fallback_실행() {
            // Given: Circuit을 OPEN 상태로 만듦
            circuitBreaker.transitionToOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When: 결제 시도
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: Fallback 메서드가 실행됨
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

            // PG는 호출되지 않음 (Circuit이 차단)
            then(pgClient).shouldHaveNoInteractions();

            // DB에 FAILED 상태로 저장됨
            PaymentEntity savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason())
                .contains("결제 시스템이 일시적으로 사용 불가능합니다");
        }

        @Test
        @DisplayName("Circuit OPEN 시 CallNotPermittedException이 발생하지 않고 Fallback 실행")
        void Circuit_OPEN_시_CallNotPermittedException이_발생하지_않고_Fallback_실행() {
            // Given: Circuit OPEN
            circuitBreaker.transitionToOpenState();

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When & Then: 예외가 발생하지 않음 (Fallback으로 처리됨)
            assertThatCode(() -> paymentFacade.processPayment(command))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("4. Fallback 메서드 실행 검증")
    class Fallback_메서드_실행_검증 {

        @Test
        @DisplayName("PG 장애 발생 시 Fallback 메서드가 실행되고 FAILED 상태로 저장")
        void PG_장애_발생_시_Fallback_메서드가_실행되고_FAILED_상태로_저장() {
            // Given: PG 호출 시 예외 발생
            given(pgClient.requestPayment(anyString(), any()))
                .willThrow(createPgException());

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When: 결제 처리
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: Fallback 결과 확인
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

            // DB 확인
            PaymentEntity savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason())
                .isNotNull()
                .contains("일시적으로 사용 불가능");
        }

        @Test
        @DisplayName("Fallback에서 생성된 결제는 transactionKey가 null이다")
        void Fallback에서_생성된_결제는_transactionKey가_null이다() {
            // Given
            given(pgClient.requestPayment(anyString(), any()))
                .willThrow(createPgException());

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: PG 호출에 실패했으므로 transactionKey가 없음
            assertThat(result.transactionKey()).isNull();

            PaymentEntity savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
            assertThat(savedPayment.getTransactionKey()).isNull();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker 메트릭 검증")
    class Circuit_Breaker_메트릭_검증 {

        @Test
        @DisplayName("성공/실패 호출 수가 정확히 기록된다")
        void 성공_실패_호출_수가_정확히_기록된다() {
            // Given
            given(pgClient.requestPayment(anyString(), any()))
                .willReturn(createSuccessResponse())
                .willReturn(createSuccessResponse())
                .willReturn(createSuccessResponse())
                .willThrow(createPgException())
                .willThrow(createPgException());

            // When: 5회 호출 (성공 3, 실패 2)
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: 메트릭 확인
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(3);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
            assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
            assertThat(metrics.getFailureRate()).isEqualTo(40.0f);
        }

        @Test
        @DisplayName("OPEN 상태에서는 notPermittedCalls가 증가한다")
        void OPEN_상태에서는_notPermittedCalls가_증가한다() {
            // Given: Circuit OPEN
            circuitBreaker.transitionToOpenState();

            // When: 3회 호출 시도
            for (int i = 0; i < 3; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                paymentFacade.processPayment(command); // Fallback 실행
            }

            // Then: notPermittedCalls = 3
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(3);
        }
    }

    // ==================== 테스트 헬퍼 메서드 ====================
    private UserInfo createAndSaveUser() {
        // Given: 사용자 생성 및 포인트 충전
        UserRegisterCommand command = createRandomUserCommand();
        return userFacade.registerUser(command);
    }

    /**
     * 랜덤 사용자명으로 UserRegisterCommand 생성
     */
    private UserRegisterCommand createRandomUserCommand() {
        String randomUsername = faker.name().firstName();
        String randomEmail = randomUsername + "@example.com";
        return UserRegisterCommand.of(randomUsername, randomEmail, "1990-01-01", Gender.MALE);
    }

    /**
     * 커스텀 UserRegisterCommand 생성
     */
    private UserRegisterCommand createUserCommand(String username) {
        String email = username + "@example.com";
        return UserRegisterCommand.of(username, email, "1990-01-01", Gender.MALE);
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

    private PaymentCommand createPaymentCommand(OrderEntity order, UserInfo user) {
        return PaymentCommand.builder()
            .username(user.username())
            .orderId(order.getId())
            .cardType("CREDIT")
            .cardNo("1234-5678-9012-3456")
            .amount(order.getFinalTotalAmount())
            .callbackUrl("http://localhost:8080/api/v1/payments/callback")
            .build();
    }

    /**
     * PG 예외 생성 (ServiceUnavailable)
     */
    private FeignException.ServiceUnavailable createPgException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://pg-simulator/api/v1/payments",
            java.util.Collections.emptyMap(),
            null,
            new RequestTemplate()
        );

        return new FeignException.ServiceUnavailable(
            "PG 서비스 일시적 장애",
            request,
            null,
            null
        );
    }

    /**
     * PG 성공 응답 생성
     */
    private PgPaymentResponse createSuccessResponse() {
        return new PgPaymentResponse(
            "tx-1234567890",
            "1",
            BigDecimal.valueOf(50000),
            "APPROVED",
            "결제 승인 완료"
        );
    }
}

