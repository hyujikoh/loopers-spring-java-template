# Loopers Spring Java 템플릿 프로젝트 - GitHub Copilot 지침

## 프로젝트 개요

Loopers는 DDD(Domain-Driven Design) 기반의 레이어드 아키텍처를 사용하는 Spring Boot 프로젝트입니다. 한국 개발팀을 위해 한국어 개발 환경을 지원하며, 확장 가능하고 유지보수 가능한 코드 구조를 목표로 합니다.

### 핵심 기술 스택
- Java 17+
- Spring Boot 3.x
- Spring Data JPA + QueryDSL
- MySQL 8.0
- Gradle

## 아키텍처 패턴

### DDD 레이어드 아키텍처

프로젝트는 4개의 주요 레이어로 구성됩니다:

```
┌─────────────────────────────────────┐
│       Interface Layer               │  ← REST API, 외부 인터페이스
├─────────────────────────────────────┤
│      Application Layer              │  ← 유스케이스 조정, Facade
├─────────────────────────────────────┤
│        Domain Layer                 │  ← 핵심 비즈니스 로직
├─────────────────────────────────────┤
│    Infrastructure Layer             │  ← 데이터 접근, 외부 시스템
└─────────────────────────────────────┘
```

### 의존성 방향 규칙

```
Interface → Application → Domain ← Infrastructure
```

- Domain Layer는 다른 계층에 의존하지 않음 (순수한 비즈니스 로직)
- Application Layer는 Domain Layer에만 의존
- Infrastructure Layer는 Domain Layer의 인터페이스를 구현
- Interface Layer는 Application Layer를 통해 Domain Layer 접근

### 패키지 구조

```
com.loopers
├── domain                    # 도메인 계층
│   ├── user
│   │   ├── UserEntity.java
│   │   ├── UserService.java
│   │   └── UserRepository.java (interface)
│   └── order
├── application              # 애플리케이션 계층
│   ├── user
│   │   ├── UserFacade.java
│   │   ├── UserInfo.java
│   │   └── UserRegisterCommand.java
│   └── order
├── infrastructure           # 인프라스트럭처 계층
│   ├── user
│   │   ├── UserRepositoryImpl.java
│   │   └── UserJpaRepository.java
│   └── order
└── interfaces              # 인터페이스 계층
    └── api
        ├── user
        │   ├── UserV1Controller.java
        │   ├── UserV1ApiSpec.java
        │   └── UserV1Dtos.java
        └── order
```

## 코딩 컨벤션

### 네이밍 규칙

- **클래스명**: PascalCase (예: `UserService`, `OrderRepository`)
- **메서드명**: camelCase, 동사로 시작 (예: `findUserById`, `createOrder`)
- **변수명**: camelCase (예: `userId`, `orderAmount`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`)
- **패키지명**: 소문자, 점(.) 구분 (예: `com.loopers.domain.user`)

### 레이어별 클래스 접미사

- **Domain Layer**
  - Entity: `~Entity` (예: `UserEntity`)
  - Service: `~Service` (예: `UserService`)
  - Repository Interface: `~Repository` (예: `UserRepository`)

- **Application Layer**
  - Facade: `~Facade` (예: `UserFacade`)
  - Command: `~Command` (예: `UserRegisterCommand`)
  - Info/DTO: `~Info` (예: `UserInfo`)

- **Infrastructure Layer**
  - Repository Impl: `~RepositoryImpl` (예: `UserRepositoryImpl`)
  - JPA Repository: `~JpaRepository` (예: `UserJpaRepository`)

- **Interface Layer**
  - Controller: `~V{version}Controller` (예: `UserV1Controller`)
  - API Spec: `~V{version}ApiSpec` (예: `UserV1ApiSpec`)
  - DTO: `~V{version}Dtos` (예: `UserV1Dtos`)

### 메서드 네이밍 패턴

```java
// 조회
find~()    // 단일 객체, Optional 반환
get~()     // 단일 객체, 필수 존재
list~()    // 목록 조회
search~()  // 검색 조건 기반

// 생성
create~()  // 새로운 객체 생성
register~() // 등록 프로세스
add~()     // 컬렉션에 추가

// 수정
update~()  // 전체/부분 수정
modify~()  // 특정 속성 수정
change~()  // 상태 변경

// 삭제
delete~()  // 물리적 삭제
remove~()  // 컬렉션에서 제거
cancel~()  // 취소 처리

// 검증
validate~() // 검증 수행
check~()    // 조건 확인
verify~()   // 인증/인가 확인

// 불린 반환
is~()      // 상태 확인
has~()     // 소유 여부
can~()     // 가능 여부
```


## 엔티티 설계

### BaseEntity 상속 패턴

모든 도메인 엔티티는 `BaseEntity`를 상속하여 공통 생명주기를 관리합니다.

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {
    
    @Column(unique = true, nullable = false, length = 10)
    private String username;
    
    @Column(nullable = false, length = 254)
    private String email;
    
    @Column(name = "point_amount", precision = 9, scale = 2, nullable = false)
    private BigDecimal pointAmount = BigDecimal.ZERO;
    
    // 정적 팩토리 메서드를 통한 생성
    public static UserEntity createUserEntity(UserDomainCreateRequest request) {
        validateCreateRequest(request);
        
        UserEntity user = new UserEntity();
        user.username = request.username();
        user.email = request.email();
        user.pointAmount = BigDecimal.ZERO;
        
        return user;
    }
    
    // 비즈니스 로직 메서드
    public void chargePoint(BigDecimal amount) {
        validateChargeAmount(amount);
        this.pointAmount = this.pointAmount.add(amount);
    }
    
    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
    }
    
    // guard() 메서드 오버라이드
    @Override
    protected void guard() {
        if (this.pointAmount != null && this.pointAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("포인트 잔액은 음수가 될 수 없습니다.");
        }
    }
}
```

### 엔티티 설계 원칙

1. **BaseEntity 상속**: 생성/수정 시간, 소프트 삭제 자동 관리
2. **정적 팩토리 메서드**: `createXxx()` 메서드로 명확한 생성 의도 표현
3. **불변성 보장**: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 외부 생성 방지
4. **비즈니스 로직 캡슐화**: 도메인 규칙을 엔티티 내부에서 검증
5. **guard() 활용**: BaseEntity의 guard() 메서드를 오버라이드하여 엔티티별 검증

### BaseEntity 특징

```java
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id = 0L;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
    
    protected void guard() {}  // 하위 클래스에서 오버라이드
    
    @PrePersist
    private void prePersist() {
        guard();
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    private void preUpdate() {
        guard();
        this.updatedAt = ZonedDateTime.now();
    }
    
    public void delete() {
        if (this.deletedAt == null) {
            this.deletedAt = ZonedDateTime.now();
        }
    }
}
```

## Repository 패턴

### 3계층 Repository 구조

1. **Domain Repository Interface** (도메인 계층)
2. **Repository Implementation** (인프라 계층)
3. **JPA Repository Interface** (인프라 계층)

### 1. Domain Repository Interface

```java
// com.loopers.domain.user.UserRepository
public interface UserRepository {
    UserEntity save(UserEntity user);
    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserEntity> findActiveUsers();
    Page<UserEntity> findActiveUsers(Pageable pageable);
}
```

### 2. Repository Implementation

```java
// com.loopers.infrastructure.user.UserRepositoryImpl
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    
    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory queryFactory;
    
    @Override
    public UserEntity save(UserEntity user) {
        return userJpaRepository.save(user);
    }
    
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userJpaRepository.findByUsername(username);
    }
    
    @Override
    public List<UserEntity> findActiveUsers() {
        return queryFactory
            .selectFrom(QUserEntity.userEntity)
            .where(
                QUserEntity.userEntity.status.eq(UserStatus.ACTIVE),
                QUserEntity.userEntity.deletedAt.isNull()
            )
            .fetch();
    }
    
    @Override
    public Page<UserEntity> findActiveUsers(Pageable pageable) {
        List<UserEntity> users = queryFactory
            .selectFrom(QUserEntity.userEntity)
            .where(
                QUserEntity.userEntity.status.eq(UserStatus.ACTIVE),
                QUserEntity.userEntity.deletedAt.isNull()
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        Long total = queryFactory
            .select(QUserEntity.userEntity.count())
            .from(QUserEntity.userEntity)
            .where(
                QUserEntity.userEntity.status.eq(UserStatus.ACTIVE),
                QUserEntity.userEntity.deletedAt.isNull()
            )
            .fetchOne();
        
        return new PageImpl<>(users, pageable, total != null ? total : 0);
    }
}
```

### 3. JPA Repository Interface

```java
// com.loopers.infrastructure.user.UserJpaRepository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM UserEntity u WHERE u.deletedAt IS NULL")
    List<UserEntity> findAllActive();
}
```

### Repository 패턴 원칙

1. **인터페이스는 Domain Layer에 정의**: 도메인 중심의 데이터 접근 인터페이스
2. **구현체는 Infrastructure Layer에 위치**: 기술적 구현 세부사항 분리
3. **QueryDSL 활용**: 복잡한 동적 쿼리는 QueryDSL로 구현
4. **페이징 최적화**: 카운트 쿼리와 데이터 조회 쿼리 분리


## Service 패턴

### Domain Service vs Application Facade

| 구분 | Domain Service | Application Facade |
|------|----------------|-------------------|
| 위치 | Domain Layer | Application Layer |
| 책임 | 단일 도메인 비즈니스 로직 | 유스케이스 조정 |
| 의존성 | Repository만 의존 | 여러 Domain Service 의존 |
| 트랜잭션 | 단순 트랜잭션 | 복합 트랜잭션 |
| 반환값 | Domain Entity | Application DTO |

### Domain Service 예시

```java
// com.loopers.domain.user.UserService
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    // 단일 도메인 비즈니스 로직
    @Transactional
    public UserEntity register(UserDomainCreateRequest request) {
        // 중복 검사
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + request.username());
        }
        
        // 도메인 객체 생성
        UserEntity user = UserEntity.createUserEntity(request);
        
        // 저장
        return userRepository.save(user);
    }
    
    public UserEntity getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));
    }
}
```

### Application Facade 예시

```java
// com.loopers.application.user.UserFacade
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFacade {
    
    private final UserService userService;
    private final PointService pointService;
    private final NotificationService notificationService;
    
    // 여러 도메인 서비스 조합
    @Transactional
    public UserInfo registerUser(UserRegisterCommand command) {
        // 1. 사용자 등록
        UserEntity user = userService.register(command.toDomainRequest());
        
        // 2. 웰컴 포인트 지급
        if (command.shouldGiveWelcomePoint()) {
            user.chargePoint(new BigDecimal("1000"));
            pointService.recordPointHistory(
                user.getId(), 
                new BigDecimal("1000"), 
                PointTransactionType.WELCOME_BONUS
            );
        }
        
        // 3. 환영 알림 발송
        notificationService.sendWelcomeNotification(user.getId());
        
        // 4. DTO 변환 후 반환
        return UserInfo.from(user);
    }
    
    public UserInfo getUserById(Long id) {
        UserEntity user = userService.getUserById(id);
        return UserInfo.from(user);
    }
}
```

### Service 패턴 원칙

1. **Domain Service**: 단일 도메인 엔티티 관련 비즈니스 로직만 처리
2. **Application Facade**: 여러 도메인 서비스를 조합하여 완전한 유스케이스 구현
3. **트랜잭션 관리**: Facade에서 복합 트랜잭션 경계 설정
4. **DTO 변환**: Facade에서 Domain Entity를 Application DTO로 변환

## 예외 처리

### CoreException 기반 예외 체계

모든 비즈니스 예외는 `CoreException`을 상속합니다.

```java
@Getter
public class CoreException extends RuntimeException {
    private final ErrorType errorType;
    private final String customMessage;

    public CoreException(ErrorType errorType) {
        this(errorType, null);
    }

    public CoreException(ErrorType errorType, String customMessage) {
        super(customMessage != null ? customMessage : errorType.getMessage());
        this.errorType = errorType;
        this.customMessage = customMessage;
    }
}
```

### ErrorType 정의

```java
@Getter
@RequiredArgsConstructor
public enum ErrorType {
    // 범용 에러
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),
    
    // 사용자 관련 에러 (U로 시작)
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, "U003", "유효하지 않은 사용자 상태입니다."),
    
    // 포인트 관련 에러 (P로 시작)
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "P001", "포인트가 부족합니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "P002", "유효하지 않은 포인트 금액입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 도메인별 예외 클래스

```java
// 사용자를 찾을 수 없는 경우
public class UserNotFoundException extends CoreException {
    public UserNotFoundException(Long userId) {
        super(ErrorType.NOT_FOUND_USER, 
              String.format("사용자를 찾을 수 없습니다. (ID: %d)", userId));
    }
    
    public UserNotFoundException(String email) {
        super(ErrorType.NOT_FOUND_USER, 
              String.format("사용자를 찾을 수 없습니다. (이메일: %s)", email));
    }
}

// 중복 이메일 예외
public class DuplicateEmailException extends CoreException {
    public DuplicateEmailException(String email) {
        super(ErrorType.DUPLICATE_EMAIL, 
              String.format("이미 사용 중인 이메일입니다. 다른 이메일을 사용해 주세요. (이메일: %s)", email));
    }
}

// 포인트 부족 예외
public class InsufficientPointsException extends CoreException {
    public InsufficientPointsException(BigDecimal currentPoints, BigDecimal requiredPoints) {
        super(ErrorType.INSUFFICIENT_POINTS, 
              String.format("포인트가 부족합니다. (보유: %s, 필요: %s)", 
                          formatPoints(currentPoints), formatPoints(requiredPoints)));
    }
    
    private static String formatPoints(BigDecimal points) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(points) + "P";
    }
}
```

### 예외 처리 원칙

1. **CoreException 상속**: 모든 비즈니스 예외는 CoreException을 상속
2. **한국어 메시지**: 사용자 친화적인 한국어 에러 메시지 제공
3. **구체적인 정보**: 에러 상황을 명확히 설명하는 구체적인 정보 포함
4. **계층별 예외 변환**: 기술적 예외를 비즈니스 예외로 변환

### 전역 예외 처리

```java
@RestControllerAdvice
@Slf4j
public class ApiControllerAdvice {
    
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handle(CoreException e) {
        log.warn("CoreException : {}", e.getCustomMessage() != null ? e.getCustomMessage() : e.getMessage(), e);
        return failureResponse(e.getErrorType(), e.getCustomMessage());
    }
    
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "유효성 검증에 실패했습니다.")
                .collect(Collectors.joining(", "));
        return failureResponse(ErrorType.BAD_REQUEST, errorMessage);
    }
    
    private ResponseEntity<ApiResponse<?>> failureResponse(ErrorType errorType, String errorMessage) {
        return ResponseEntity.status(errorType.getStatus())
            .body(ApiResponse.fail(errorType.getCode(), errorMessage != null ? errorMessage : errorType.getMessage()));
    }
}
```

## 테스팅 표준

### 테스트 네이밍 규칙

모든 테스트는 한국어로 작성하며, 비즈니스 의도를 명확히 표현합니다.

#### 테스트 클래스 네이밍

```java
// 단위 테스트
{대상클래스명}Test
// 예시: UserServiceTest, OrderRepositoryTest

// 통합 테스트
{대상클래스명}IntegrationTest
// 예시: UserServiceIntegrationTest

// E2E 테스트
{대상클래스명}E2ETest
// 예시: UserV1ApiE2ETest
```

#### 테스트 메서드 네이밍

**패턴**: `{전제조건}_{행동}_{기대결과}`

```java
@DisplayName("UserService 테스트")
class UserServiceTest {
    
    @Nested
    @DisplayName("사용자 등록")
    class 사용자_등록 {
        
        @Test
        @DisplayName("유효한 사용자 정보로 등록하면 성공한다")
        void 유효한_사용자_정보로_등록하면_성공한다() {
            // Given
            UserRegisterCommand command = UserTestFixture.createRegisterCommand();
            
            // When
            UserInfo result = userService.register(command);
            
            // Then
            assertThat(result.getEmail()).isEqualTo(command.getEmail());
        }
        
        @Test
        @DisplayName("중복된 이메일로 등록 시도하면 DuplicateEmailException이 발생한다")
        void 중복된_이메일로_등록_시도하면_DuplicateEmailException이_발생한다() {
            // Given
            String duplicateEmail = "duplicate@example.com";
            UserEntity existingUser = UserTestFixture.create(duplicateEmail);
            userRepository.save(existingUser);
            
            UserRegisterCommand command = new UserRegisterCommand(duplicateEmail, "새사용자", Gender.MALE);
            
            // When & Then
            assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다. 다른 이메일을 사용해 주세요. (이메일: " + duplicateEmail + ")");
        }
    }
}
```

### Given-When-Then 패턴

모든 테스트는 Given-When-Then 구조를 따릅니다.

```java
@Test
@DisplayName("포인트 충전 후 잔액이 증가한다")
void 포인트_충전_후_잔액이_증가한다() {
    // Given - 테스트 데이터 준비
    UserEntity user = UserTestFixture.create();
    BigDecimal initialAmount = user.getPointAmount();
    BigDecimal chargeAmount = new BigDecimal("10000");
    
    // When - 테스트 대상 메서드 실행
    user.chargePoint(chargeAmount);
    
    // Then - 결과 검증
    assertThat(user.getPointAmount()).isEqualTo(initialAmount.add(chargeAmount));
}
```

### @Nested를 활용한 테스트 구조화

```java
@DisplayName("UserService 테스트")
class UserServiceTest {
    
    @Nested
    @DisplayName("사용자 등록")
    class 사용자_등록 {
        
        @Nested
        @DisplayName("성공 케이스")
        class 성공_케이스 {
            
            @Test
            @DisplayName("유효한 정보로 등록하면 성공한다")
            void 유효한_정보로_등록하면_성공한다() {
                // 테스트 로직
            }
        }
        
        @Nested
        @DisplayName("실패 케이스")
        class 실패_케이스 {
            
            @Test
            @DisplayName("중복된 이메일로 등록 시 예외가 발생한다")
            void 중복된_이메일로_등록_시_예외가_발생한다() {
                // 테스트 로직
            }
        }
    }
}
```

### TestFixture 패턴

테스트 데이터는 TestFixture 클래스를 통해 관리합니다.

```java
public class UserTestFixture {
    
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    
    public static UserEntity createEntity() {
        return createEntity("testuser" + ID_GENERATOR.getAndIncrement());
    }
    
    public static UserEntity createEntity(String username) {
        return UserEntity.builder()
            .username(username)
            .email(username + "@example.com")
            .name("테스트사용자")
            .birthdate(LocalDate.of(1990, 1, 1))
            .gender(Gender.MALE)
            .build();
    }
    
    public static UserRegisterCommand createRegisterCommand() {
        String username = "testuser" + ID_GENERATOR.getAndIncrement();
        return new UserRegisterCommand(
            username,
            username + "@example.com",
            "1990-01-01",
            Gender.MALE
        );
    }
    
    // 빌더 패턴을 활용한 유연한 테스트 데이터 생성
    public static UserEntityBuilder builder() {
        return new UserEntityBuilder();
    }
    
    public static class UserEntityBuilder {
        private String username = "testuser";
        private String email = "test@example.com";
        private BigDecimal pointAmount = BigDecimal.ZERO;
        
        public UserEntityBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public UserEntityBuilder vip() {
            this.pointAmount = new BigDecimal("100000");
            return this;
        }
        
        public UserEntity build() {
            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setPointAmount(pointAmount);
            return user;
        }
    }
}
```

### 테스트 유형별 가이드

#### 1. 단위 테스트 (Unit Test)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceUnitTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("사용자 등록 시 저장소에 저장된다")
    void 사용자_등록_시_저장소에_저장된다() {
        // Given
        UserRegisterCommand command = UserTestFixture.createRegisterCommand();
        UserEntity savedUser = UserTestFixture.create();
        
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        
        // When
        UserInfo result = userService.register(command);
        
        // Then
        assertThat(result.getEmail()).isEqualTo(command.getEmail());
        verify(userRepository).save(any(UserEntity.class));
    }
}
```

#### 2. 통합 테스트 (Integration Test)

```java
@SpringBootTest
@Transactional
@DisplayName("UserService 통합 테스트")
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("사용자 등록 후 데이터베이스에서 조회가 가능하다")
    void 사용자_등록_후_데이터베이스에서_조회가_가능하다() {
        // Given
        UserRegisterCommand command = UserTestFixture.createRegisterCommand();
        
        // When
        UserInfo registered = userService.register(command);
        Optional<UserEntity> found = userRepository.findById(registered.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(command.getEmail());
    }
}
```

#### 3. E2E 테스트 (End-to-End Test)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("User API E2E 테스트")
class UserV1ApiE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("사용자 등록부터 조회까지 전체 플로우가 정상 동작한다")
    void 사용자_등록부터_조회까지_전체_플로우가_정상_동작한다() {
        // Given - 사용자 등록 요청 데이터
        UserV1Dtos.RegisterRequest registerRequest = new UserV1Dtos.RegisterRequest(
            "testuser", "test@example.com", "1990-01-01", "MALE"
        );
        
        // When - 사용자 등록 API 호출
        ResponseEntity<ApiResponse<UserV1Dtos.UserResponse>> registerResponse = 
            restTemplate.postForEntity("/api/v1/users", registerRequest, 
                new ParameterizedTypeReference<>() {});
        
        // Then - 등록 성공 검증
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody().getMeta().getResult()).isEqualTo(SUCCESS);
        
        String username = registerResponse.getBody().getData().getUsername();
        
        // When - 등록된 사용자 조회 API 호출
        ResponseEntity<ApiResponse<UserV1Dtos.UserResponse>> getResponse = 
            restTemplate.getForEntity("/api/v1/users/" + username, 
                new ParameterizedTypeReference<>() {});
        
        // Then - 조회 성공 및 데이터 일치 검증
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getData().getEmail()).isEqualTo(registerRequest.getEmail());
    }
}
```

## 시간 처리

### ZonedDateTime 사용

모든 시간 데이터는 `ZonedDateTime`을 사용하며, 한국 표준시(Asia/Seoul)를 기본으로 합니다.

#### 시간대 설정

```java
@Configuration
public class TimeZoneConfig {
    
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    
    @PostConstruct
    public void setKoreanTimeZone() {
        // JVM 기본 타임존을 한국 표준시로 설정
        TimeZone.setDefault(TimeZone.getTimeZone(KOREA_ZONE));
        
        log.info("시스템 타임존이 한국 표준시(Asia/Seoul)로 설정되었습니다.");
    }
    
    @Bean
    public Clock koreanClock() {
        return Clock.system(KOREA_ZONE);
    }
}
```

#### BaseEntity에서의 시간 처리

```java
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
    
    @PrePersist
    private void prePersist() {
        guard();
        ZonedDateTime now = ZonedDateTime.now(KOREA_ZONE);
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    private void preUpdate() {
        guard();
        this.updatedAt = ZonedDateTime.now(KOREA_ZONE);
    }
    
    public void delete() {
        if (this.deletedAt == null) {
            this.deletedAt = ZonedDateTime.now(KOREA_ZONE);
        }
    }
}
```

#### 비즈니스 로직에서의 시간 처리

```java
@Service
public class UserService {
    
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    
    /**
     * 사용자 나이 계산 (한국 시간 기준)
     */
    public int calculateAge(UserEntity user) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        LocalDate birthdate = user.getBirthdate();
        
        return Period.between(birthdate, today).getYears();
    }
    
    /**
     * 특정 기간 내 가입한 사용자 조회
     */
    public List<UserEntity> findUsersRegisteredBetween(LocalDate startDate, LocalDate endDate) {
        // 한국 시간 기준으로 시작일과 종료일 설정
        ZonedDateTime startDateTime = startDate.atStartOfDay(KOREA_ZONE);
        ZonedDateTime endDateTime = endDate.plusDays(1).atStartOfDay(KOREA_ZONE);
        
        return userRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }
}
```

#### 한국어 날짜 포맷팅

```java
@Component
public class KoreanDateFormatter {
    
    private static final DateTimeFormatter KOREAN_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    
    private static final DateTimeFormatter KOREAN_DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    
    /**
     * 한국어 날짜 형식으로 포맷팅
     */
    public String formatDate(ZonedDateTime dateTime) {
        return dateTime.format(KOREAN_DATE_FORMATTER);
    }
    
    /**
     * 한국어 날짜시간 형식으로 포맷팅
     */
    public String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(KOREAN_DATETIME_FORMATTER);
    }
    
    /**
     * 상대적 시간 표현 (예: 3분 전, 2시간 전)
     */
    public String formatRelativeTime(ZonedDateTime dateTime) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        Duration duration = Duration.between(dateTime, now);
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "방금 전";
        } else if (seconds < 3600) {
            return (seconds / 60) + "분 전";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "시간 전";
        } else {
            long days = seconds / 86400;
            if (days < 7) {
                return days + "일 전";
            } else if (days < 30) {
                return (days / 7) + "주 전";
            } else {
                return (days / 30) + "개월 전";
            }
        }
    }
}
```

## 한국어 개발 환경

### 한국어 에러 메시지

모든 에러 메시지는 사용자 친화적인 한국어로 작성합니다.

```java
// ✅ 좋은 예시 - 구체적이고 도움이 되는 메시지
public class UserNotFoundException extends CoreException {
    public UserNotFoundException(Long userId) {
        super(ErrorType.NOT_FOUND_USER, 
              "사용자를 찾을 수 없습니다. (ID: " + userId + ")");
    }
}

public class InsufficientPointsException extends CoreException {
    public InsufficientPointsException(BigDecimal currentPoints, BigDecimal requiredPoints) {
        super(ErrorType.INSUFFICIENT_POINTS, 
              String.format("포인트가 부족합니다. (보유: %s, 필요: %s)", 
                          formatPoints(currentPoints), formatPoints(requiredPoints)));
    }
}

// ❌ 나쁜 예시 - 모호하고 도움이 되지 않는 메시지
public class BadExample {
    throw new RuntimeException("Error"); // 영어 사용, 의도 불명확
    throw new RuntimeException("오류"); // 너무 모호함
}
```

### 한국어 로그 메시지

```java
@Service
@Slf4j
public class UserService {
    
    @Transactional
    public UserEntity register(UserDomainCreateRequest request) {
        // INFO: 비즈니스 흐름 추적
        log.info("사용자 등록 시작 - 이메일: {}, 사용자명: {}", 
                request.email(), request.username());
        
        try {
            UserEntity user = performRegistration(request);
            
            // INFO: 성공적인 비즈니스 이벤트
            log.info("사용자 등록 완료 - ID: {}, 이메일: {}", 
                    user.getId(), user.getEmail());
            
            return user;
            
        } catch (DuplicateEmailException e) {
            // WARN: 예상 가능한 비즈니스 예외
            log.warn("사용자 등록 실패 - 중복 이메일: {}", request.email());
            throw e;
            
        } catch (Exception e) {
            // ERROR: 예상하지 못한 시스템 오류
            log.error("사용자 등록 중 예상하지 못한 오류 발생 - 이메일: {}", 
                     request.email(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, 
                "사용자 등록 중 오류가 발생했습니다.");
        }
    }
}
```

### 한국어 주석

비즈니스 도메인 관련 주석은 한국어로 작성합니다.

```java
/**
 * 사용자 포인트 차감 처리
 * 
 * 포인트 차감 시 다음 규칙을 적용합니다:
 * 
 *   차감 금액은 0보다 커야 함
 *   보유 포인트보다 많이 차감할 수 없음
 *   차감 이력을 포인트 히스토리에 기록
 * 
 */
public void deductPoints(BigDecimal amount) {
    // 포인트 차감 가능 여부 검증
    validatePointDeduction(amount);
    
    // 포인트 차감 및 이력 기록
    this.pointAmount = this.pointAmount.subtract(amount);
    addPointHistory(amount, PointTransactionType.DEDUCT);
}
```

## 참고 문서

프로젝트의 상세한 표준과 가이드는 `.kiro/steering` 디렉토리의 문서를 참조하세요:

- **아키텍처 패턴**: `.kiro/steering/architecture-patterns.md`
- **자바 코딩 컨벤션**: `.kiro/steering/java-conventions.md`
- **데이터베이스 패턴**: `.kiro/steering/database-patterns.md`
- **예외 처리 표준**: `.kiro/steering/error-handling.md`
- **테스팅 표준**: `.kiro/steering/testing-standards.md`
- **한국어 개발 환경**: `.kiro/steering/korean-development.md`
- **인프라 구성**: `.kiro/steering/infrastructure-setup.md`
- **코드 리뷰 체크리스트**: `.kiro/steering/code-review-checklist.md`

