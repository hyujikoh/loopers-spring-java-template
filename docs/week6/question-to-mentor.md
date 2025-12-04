# 멘토님께 질문 - PG 결제 연동 시 트랜잭션 불일치 문제

# 멘토님께 질문 - PG 결제 연동 시 트랜잭션 불일치 문제

## 📌 질문 1: 외부 API 성공 후 내부 로직 실패 시 정합성 문제

### 🎯 TL;DR

> **현재 Resilience4j(CircuitBreaker, Retry, TimeLimiter)를 적용했는데, 이것만으로 "PG는 결제 성공, 우리 DB는 롤백"되는 분산 트랜잭션 불일치 문제를 해결할 수 있나요?**
> 
> **Quest의 "콜백 방식 + 결제 상태 확인 API"가 Fallback + 스케줄러 패턴을 의미하는 건가요, 아니면 보상 트랜잭션(PG 취소 API)을 의미하나요?**

### 🔍 핵심 문제

현재 `@Transactional` 메서드 내에서 **외부 PG API 호출 → 내부 DB 저장**을 순차적으로 처리하고 있습니다.

**문제 상황:**
- PG 결제 API 호출 성공 ✅ → PG 시스템에 결제 데이터 저장됨
- 내부 비즈니스 로직 실패 ❌ → `@Transactional` 롤백 발생
- **결과:** PG는 결제 완료 상태, 우리 DB는 결제 정보 없음 (데이터 불일치)

**반대 상황도 가능:**
- PG TimeLimiter 10초 타임아웃 발생 → 우리는 실패 처리
- 실제 PG 시스템은 11초에 결제 완료 처리
- **결과:** 고객은 "결제 실패" 메시지 받았는데 카드사 승인 문자는 옴

### 💻 현재 코드 구조

```java
// PgClient.java - Resilience4j 적용
@FeignClient(name = "pgClient", url = "${pg.simulator.url}")
public interface PgClient {
    @CircuitBreaker(name = "pgClient")
    @Retry(name = "pgClient", maxAttempts = 3)
    @TimeLimiter(name = "pgClient", timeout = 10s)
    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(...);
}

// 사용하는 곳 (가정)
@Transactional
public PaymentInfo processPayment(PaymentCommand command) {
    // 1. 외부 API 호출 (PG 서버에 데이터 커밋됨)
    PgPaymentResponse pgResponse = pgClient.requestPayment(userId, request);
    
    // 2. 내부 DB 저장
    PaymentEntity payment = paymentService.createPayment(pgResponse);
    
    // 3. 재고 차감 및 주문 상태 변경
    inventoryService.decrease(productId, quantity);  // ❌ 재고 부족 예외 발생!
    orderService.updateStatus(orderId, PAID);
    
    // → @Transactional 롤백 발생
    // → payment 저장 취소, 재고도 원복
    // → 하지만 PG는 이미 결제 완료 상태!
}
```

### 🤔 제가 생각한 해결 방안

#### 방안 1: 보상 트랜잭션 (Compensating Transaction)

```java
@Transactional
public PaymentInfo processPayment(PaymentCommand command) {
    PgPaymentResponse pgResponse = null;
    
    try {
        // 1. PG 결제
        pgResponse = pgClient.requestPayment(userId, request);
        
        // 2. 내부 로직
        PaymentEntity payment = paymentService.createPayment(pgResponse);
        inventoryService.decrease(productId, quantity);
        orderService.updateStatus(orderId, PAID);
        
        return PaymentInfo.from(payment);
        
    } catch (Exception e) {
        // 롤백 시 PG 취소 API 호출 (보상 트랜잭션)
        if (pgResponse != null && pgResponse.isSuccess()) {
            pgClient.cancelPayment(pgResponse.getTransactionKey());
        }
        throw e;
    }
}
```

**의문점:**
- PG 취소 API 호출도 실패하면 어떻게 하나요?
- 취소 불가능한 상태(정산 시작, 배치 마감 등)라면?
- 보상 트랜잭션 자체가 또 다른 분산 트랜잭션 문제를 만드는 건 아닌가요?

#### 방안 2: 외부 API를 트랜잭션 밖으로

```java
public PaymentInfo processPayment(PaymentCommand command) {
    // 1. 트랜잭션 밖에서 PG 호출
    PgPaymentResponse pgResponse = pgClient.requestPayment(userId, request);
    
    // 2. 별도 트랜잭션으로 내부 저장
    return savePaymentWithTransaction(pgResponse, command);
}

@Transactional
private PaymentInfo savePaymentWithTransaction(PgPaymentResponse pgResponse, PaymentCommand command) {
    PaymentEntity payment = paymentService.createPayment(pgResponse);
    inventoryService.decrease(...);
    orderService.updateStatus(...);
    return PaymentInfo.from(payment);
}
```

**의문점:**
- 내부 로직 실패 시 PG는 이미 성공했는데, 결국 보상 트랜잭션이 필요한 건 같은 문제 아닌가요?
- 트랜잭션 범위만 나눴을 뿐 불일치 문제는 동일하지 않나요?

#### 방안 3: 순서 변경 (검증 먼저)

```java
@Transactional
public PaymentInfo processPayment(PaymentCommand command) {
    // 1. 재고 등 내부 검증 먼저 (락 잡기)
    inventoryService.validateAndReserve(productId, quantity);
    orderService.validateOrderStatus(orderId);
    
    // 2. 검증 통과 후 PG 호출
    PgPaymentResponse pgResponse = pgClient.requestPayment(userId, request);
    
    // 3. DB 저장
    PaymentEntity payment = paymentService.createPayment(pgResponse);
    orderService.updateStatus(orderId, PAID);
    
    return PaymentInfo.from(payment);
}
```

**의문점:**
- 검증 시점과 PG 호출 사이에 다른 요청이 재고를 소진하면 어떻게 하나요?
- PG 호출이 실패하면 예약한 재고를 다시 풀어줘야 하는데, 이것도 보상 트랜잭션 아닌가요?

### 🤔 Resilience4j로 이 문제를 해결할 수 있을까?

현재 구현된 Resilience4j 설정을 보면:

```java
@CircuitBreaker(name = "pgClient")
@Retry(name = "pgClient", maxAttempts = 3)
@TimeLimiter(name = "pgClient", timeout = 10s)
@PostMapping("/api/v1/payments")
PgPaymentResponse requestPayment(...);

@GetMapping("/api/v1/payments/{transactionKey}")
PgPaymentResponse getPayment(...);  // 결제 상태 확인 API
```

Quest 문서의 요구사항:
> "콜백 방식 + **결제 상태 확인 API**를 활용해 적절하게 시스템과 결제정보를 연동한다."
> "PG 에 대한 요청이 타임아웃에 의해 실패되더라도 해당 결제건에 대한 정보를 확인하여 정상적으로 시스템에 반영한다."

**제가 생각한 Resilience4j 기반 해결 방안:**

#### Option 1: Fallback에서 PENDING 저장 + 스케줄러로 복구

```java
@Component
public class PaymentFacade {
    @CircuitBreaker(name = "pgClient", fallbackMethod = "processPaymentFallback")
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        // PG 호출
        PgPaymentResponse pgResponse = pgClient.requestPayment(...);
        
        // 성공 시 정상 플로우
        PaymentEntity payment = paymentService.createPayment(pgResponse);
        inventoryService.decrease(...);
        
        return PaymentInfo.from(payment);
    }
    
    // Fallback: 타임아웃/실패 시 PENDING 저장
    private PaymentInfo processPaymentFallback(PaymentCommand command, Throwable t) {
        log.warn("PG 결제 실패/타임아웃, PENDING 상태로 저장", t);
        
        PaymentEntity pendingPayment = paymentService.createPending(command);
        return PaymentInfo.pending(pendingPayment);
    }
}

// 별도 스케줄러: PENDING 결제건 복구
@Scheduled(fixedDelay = 10000)  // 10초마다
public void recoverPendingPayments() {
    List<PaymentEntity> pendings = paymentRepository.findAllPending();
    
    for (PaymentEntity payment : pendings) {
        try {
            // 결제 상태 확인 API로 실제 상태 조회
            PgPaymentResponse pgStatus = pgClient.getPayment(
                payment.getUserId(), 
                payment.getTransactionKey()
            );
            
            if (pgStatus.isSuccess()) {
                // PG는 성공했다면 → 정상 처리
                paymentService.confirmPayment(payment.getId());
                inventoryService.decrease(...);
                orderService.updateStatus(..., PAID);
            } else if (pgStatus.isFailed()) {
                // PG도 실패했다면 → 실패 처리
                paymentService.failPayment(payment.getId());
            }
            // PENDING이면 계속 대기
        } catch (Exception e) {
            log.error("결제 복구 실패: {}", payment.getId(), e);
        }
    }
}
```

**이 방식의 장단점:**
- ✅ TimeLimiter 타임아웃 후 실제 PG 성공 케이스도 복구 가능
- ✅ Circuit Open 시에도 fallback으로 PENDING 저장
- ✅ Quest의 "결제 상태 확인 API" 요구사항 충족
- ❌ 하지만 **내부 로직(재고) 실패 시** 문제는 여전히 존재
  - PG는 성공 → 재고 부족 → 롤백 → 스케줄러가 다시 복구 → 재고 여전히 부족 → 무한 반복

#### Option 2: 순서 변경 + Fallback

```java
@Transactional
public PaymentInfo processPayment(PaymentCommand command) {
    // 1. 내부 검증 먼저 (재고, 주문 상태 등)
    inventoryService.validateAndReserve(productId, quantity);
    
    // 2. 검증 통과 후 PG 호출
    PgPaymentResponse pgResponse = pgClient.requestPayment(...);
    
    // 3. DB 저장
    PaymentEntity payment = paymentService.createPayment(pgResponse);
    
    return PaymentInfo.from(payment);
}
```

**이 방식의 문제:**
- ❌ 검증 통과 → PG 호출 사이에 다른 요청이 재고 소진 가능
- ❌ PG 성공 후 롤백되면 결국 같은 문제

### ❓ 멘토님께 질문드립니다

**Resilience4j를 사용하고 있는데, 이것만으로 "외부 API 성공 후 내부 로직 실패" 문제를 해결할 수 있을까요?**

제가 보기엔:
- **Resilience4j는 "외부 시스템 장애"를 감지하고 대응**하는 도구 (Circuit Breaker, Retry, Timeout)
- **하지만 "내부 비즈니스 로직 실패"와 "외부 API 성공"의 불일치**는 근본적으로 다른 문제

Quest 문서를 보면 **"콜백 방식 + 결제 상태 확인 API"**가 핵심인 것 같은데:
1. 이게 제가 생각한 **"PENDING 저장 → 스케줄러로 복구"** 패턴을 의미하는 건가요?
2. 아니면 Resilience4j와는 별개로 **보상 트랜잭션**(PG 취소 API)을 구현해야 하나요?
3. 실무에서는 이런 불일치를 "어느 정도 허용"하고 모니터링 + 수동 복구하시나요?

**추가 의문:**
- PG 성공 → 재고 부족으로 롤백 → 스케줄러가 복구 시도 → 재고 여전히 부족 → 이런 케이스는 어떻게 처리하나요?
- Circuit이 Open 상태에서 PENDING으로 저장된 결제건은 언제 복구를 시도해야 하나요? (Circuit이 Half-Open될 때까지 대기?)

---

## 📌 질문 2: PgClientAdapter(Gateway) 패턴의 실무적 필요성

### 🔍 핵심 의문

Application Layer(Facade)와 Infrastructure Layer(FeignClient) 사이에 **Adapter를 두는 것이 실제로 필요한가요?**

현재는 Adapter가 단순히 PgClient를 래핑만 하는 것 같아서 **오버 엔지니어링**처럼 느껴집니다.

### 💻 현재 구조 vs 제안 구조

#### 현재: Facade가 FeignClient 직접 호출

```java
// Application Layer
@Component
public class PaymentFacade {
    private final PgClient pgClient;  // Infrastructure Layer 직접 의존
    
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        // PG 호출
        PgPaymentResponse pgResponse = pgClient.requestPayment(
            command.getUserId(), 
            PgPaymentRequest.of(...)
        );
        
        // 도메인 저장
        PaymentEntity payment = paymentService.createPayment(pgResponse);
        
        return PaymentInfo.from(payment);
    }
}

// Infrastructure Layer
@FeignClient(name = "pgClient", url = "${pg.simulator.url}")
public interface PgClient {
    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PgPaymentRequest request
    );
}
```

**장점:**
- 구조가 단순하고 직관적
- 레이어가 적어서 코드 추적이 쉬움

**단점:**
- Application Layer가 Feign 구현에 직접 의존
- PgPaymentResponse(Infrastructure DTO)가 Facade까지 침투
- 여러 곳에서 호출 시 중복 코드 발생 가능

#### 제안: Adapter 추가

```java
// Application Layer
@Component
public class PaymentFacade {
    private final PgClientAdapter pgClientAdapter;  // Adapter 의존
    
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        // Adapter를 통한 호출
        PgPaymentResponse pgResponse = pgClientAdapter.requestPayment(
            command.getUserId(),
            command.getOrderId(),
            command.getAmount()
        );
        
        PaymentEntity payment = paymentService.createPayment(pgResponse);
        return PaymentInfo.from(payment);
    }
}

// Infrastructure Layer - Adapter
@Component
public class PgClientAdapter {
    private final PgClient pgClient;
    
    public PgPaymentResponse requestPayment(String userId, String orderId, BigDecimal amount) {
        PgPaymentRequest request = PgPaymentRequest.of(orderId, "CARD", "****", amount, "callback");
        return pgClient.requestPayment(userId, request);
    }
}

// Infrastructure Layer - FeignClient
@FeignClient(name = "pgClient", url = "${pg.simulator.url}")
public interface PgClient {
    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PgPaymentRequest request
    );
}
```

**장점:**
- Facade가 Feign 구현을 몰라도 됨
- PgPaymentRequest 생성 로직을 Adapter에 캡슐화
- 예외 변환, 로깅 등 부가 기능 추가 용이

**단점:**
- 레이어 하나 추가로 복잡도 증가
- 단순 래핑만 한다면 불필요한 코드

### 🤔 Adapter가 의미 있는 경우

#### Case 1: 여러 PG를 추상화해야 할 때

```java
// 인터페이스
public interface PaymentGateway {
    PaymentResult requestPayment(String userId, String orderId, BigDecimal amount);
}

// 토스페이 구현체
@Component
public class TossPayAdapter implements PaymentGateway {
    private final TossFeignClient tossClient;
    
    public PaymentResult requestPayment(...) {
        TossPaymentResponse response = tossClient.pay(...);
        return PaymentResult.from(response);  // 통일된 도메인 모델로 변환
    }
}

// 나이스페이 구현체
@Component
public class NicePayAdapter implements PaymentGateway {
    private final NiceFeignClient niceClient;
    
    public PaymentResult requestPayment(...) {
        NicePaymentResponse response = niceClient.payment(...);
        return PaymentResult.from(response);  // 통일된 도메인 모델로 변환
    }
}
```

이 경우는 **명확히 필요**합니다.

#### Case 2: Feign 예외를 도메인 예외로 변환할 때

```java
@Component
public class PgClientAdapter {
    private final PgClient pgClient;
    
    public PgPaymentResponse requestPayment(...) {
        try {
            return pgClient.requestPayment(userId, request);
        } catch (FeignException.ServiceUnavailable e) {
            throw new PgServiceUnavailableException("PG 시스템 장애", e);
        } catch (FeignException.BadRequest e) {
            throw new InvalidPaymentRequestException("잘못된 결제 요청", e);
        }
    }
}
```

하지만 **FallbackFactory에서도 가능**한 작업입니다.

#### Case 3: PG 응답을 도메인 모델로 변환할 때

```java
@Component
public class PgClientAdapter {
    private final PgClient pgClient;
    
    // Infrastructure DTO → Domain Model 변환
    public PaymentResult requestPayment(...) {
        PgPaymentResponse pgResponse = pgClient.requestPayment(...);
        
        return PaymentResult.builder()
            .transactionId(pgResponse.transactionKey())
            .orderId(pgResponse.orderId())
            .amount(pgResponse.amount())
            .status(convertStatus(pgResponse.status()))
            .build();
    }
    
    private PaymentStatus convertStatus(String pgStatus) {
        return switch (pgStatus) {
            case "SUCCESS" -> PaymentStatus.COMPLETED;
            case "PENDING" -> PaymentStatus.PENDING;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + pgStatus);
        };
    }
}
```

이 경우도 **유용**합니다.

### ❓ 멘토님께 질문드립니다

**현재처럼 단일 PG만 연동하고, 단순히 PgPaymentResponse를 그대로 사용하는 상황에서도 Adapter를 두는 것이 필요한가요?**

제가 보기에는:
- **여러 PG 추상화**: 현재는 해당 없음
- **예외 변환**: FallbackFactory로 가능
- **DTO 변환**: 현재는 PgPaymentResponse를 그대로 사용 중

이런 상황에서 Adapter는 **"미래를 위한 과도한 설계"**처럼 보이는데, 실무에서는 어떻게 판단하시나요?

**추가 의문:**
- DDD 관점에서 Application Layer가 Infrastructure DTO(PgPaymentResponse)를 직접 다루는 게 문제인가요?
- 아니면 "일단 단순하게 시작하고, 필요할 때 Adapter를 추가"하는 게 현실적인가요?

