# Redis 캐시 전략 문서


## 개요

상품 API의 성능 향상을 위해 Redis 캐시를 적용합니다. 이 문서는 캐시 키 설계, TTL 전략, 무효화 전략을 정의합니다.

## 캐시 키 설계

### 네이밍 규칙

모든 캐시 키는 다음 패턴을 따릅니다:
```
{도메인}:{타입}:{식별자}[:{추가정보}]
```

### 상품 도메인 캐시 키

#### 1. 상품 상세 캐시
```
패턴: product:detail:{productId}
예시: product:detail:123
```

**사용 시점**: 상품 상세 조회 API (`GET /api/v1/products/{productId}`)

**저장 데이터**: `ProductDetailInfo` 객체
- 상품 기본 정보
- 브랜드 정보
- 좋아요 여부 (사용자별)

#### 2. 상품 목록 캐시
```
패턴: product:list:{brandId}:{productName}:{page}:{size}:{sort}
예시: 
  - product:list:1:null:0:20:likeCount_desc
  - product:list:null:테스트상품:0:20:unsorted
  - product:list:1:인기상품:2:50:likeCount_desc,id_asc
```

**사용 시점**: 상품 목록 조회 API (`GET /api/v1/products`)

**저장 데이터**: `Page<ProductInfo>` 객체
- 상품 목록
- 페이징 정보 (totalElements, totalPages, etc.)

**키 구성 요소**:
- `brandId`: 브랜드 ID (없으면 "null")
- `productName`: 상품명 검색어 (없으면 "null", 특수문자 제거, 최대 50자)
- `page`: 페이지 번호
- `size`: 페이지 크기
- `sort`: 정렬 조건 (예: "likeCount_desc,id_asc" 또는 "unsorted")

## TTL (Time To Live) 전략

### TTL 설정 근거

| 캐시 타입 | TTL | 근거 |
|----------|-----|------|
| 상품 상세 | 30분 | - 상품 정보는 자주 변경되지 않음<br>- 재고 변경은 실시간 반영 필요 없음<br>- 사용자별 좋아요 상태는 변경 시 무효화 |
| 상품 목록 | 5분 | - 목록은 상세보다 변경 빈도 높음<br>- 신규 상품 등록 시 빠른 반영 필요<br>- 좋아요 수 변경 시 빠른 반영 필요 |

### TTL 조정 가이드

**TTL을 늘려야 하는 경우**:
- 캐시 히트율이 낮은 경우
- DB 부하가 높은 경우
- 데이터 변경 빈도가 낮은 경우

**TTL을 줄여야 하는 경우**:
- 데이터 정합성이 중요한 경우
- 실시간성이 요구되는 경우
- 메모리 사용량이 높은 경우

## 캐시 무효화 전략

### 1. 상품 상세 캐시 무효화

#### 무효화 시점

| 이벤트 | 무효화 대상 | 무효화 방법 |
|--------|------------|-----------|
| 상품 정보 수정 | 해당 상품 상세 | `product:detail:{productId}` 삭제 |
| 상품 삭제 | 해당 상품 상세 | `product:detail:{productId}` 삭제 |
| 재고 변경 | 해당 상품 상세 | `product:detail:{productId}` 삭제 |
| 좋아요 추가/취소 | 해당 상품 상세 | `product:detail:{productId}` 삭제 |

#### 구현 위치
- `ProductFacade` 또는 `ProductService`의 수정/삭제 메서드
- 재고 차감/복구 메서드
- 좋아요 서비스

### 2. 상품 목록 캐시 무효화

#### 무효화 전략 비교

| 전략 | 장점 | 단점 | 적용 시나리오 |
|------|------|------|--------------|
| **전체 삭제** | 구현 간단, 정합성 보장 | 캐시 히트율 급감 | 상품 등록/삭제 |
| **브랜드별 삭제** | 영향 범위 최소화 | 패턴 매칭 비용 | 특정 브랜드 상품 수정 |
| **TTL 의존** | 무효화 로직 불필요 | 정합성 지연 | 좋아요 수 변경 |

#### 무효화 시점

| 이벤트 | 무효화 대상 | 무효화 방법 |
|--------|------------|-----------|
| 상품 등록 | 전체 목록 | `product:list:*` 패턴 삭제 |
| 상품 삭제 | 전체 목록 | `product:list:*` 패턴 삭제 |
| 상품 정보 수정 | 해당 브랜드 목록 | `product:list:{brandId}:*` 패턴 삭제 |
| 좋아요 변경 | 무효화 안함 | TTL 만료 대기 (5분) |

#### 구현 위치
- `ProductFacade`의 등록/수정/삭제 메서드

## 캐시 미스 처리

### 처리 흐름

```
1. 캐시 조회 시도
   ↓
2. 캐시 히트?
   ├─ Yes → 캐시 데이터 반환
   └─ No  → 3단계로
   ↓
3. DB 조회
   ↓
4. 조회 결과를 캐시에 저장
   ↓
5. 결과 반환
```

### 예외 처리 원칙

**캐시 실패는 서비스 실패가 아니다**

- 모든 캐시 작업은 try-catch로 감싸기
- 캐시 실패 시 로그만 남기고 계속 진행
- DB 조회는 항상 성공해야 함

```java
// ✅ 올바른 예시
Optional<ProductDetailInfo> cached = cacheService.getProductDetailFromCache(productId);
if (cached.isPresent()) {
    return cached.get(); // 캐시 히트
}

// 캐시 미스 - DB 조회
ProductDetailInfo detail = loadFromDatabase(productId);

// 캐시 저장 (실패해도 무방)
cacheService.cacheProductDetail(productId, detail);

return detail;
```

## 캐시 모니터링

### 주요 메트릭

1. **캐시 히트율**
   - 목표: 상품 상세 70% 이상, 상품 목록 50% 이상
   - 측정: `(캐시 히트 수 / 전체 조회 수) * 100`

2. **평균 응답 시간**
   - 캐시 히트: 10ms 이하
   - 캐시 미스: 100ms 이하

3. **메모리 사용량**
   - Redis 메모리 사용량 모니터링
   - 키 개수 추적

### 로그 레벨

- **DEBUG**: 캐시 히트/미스, 저장/삭제 성공
- **WARN**: 캐시 작업 실패 (JSON 오류, Redis 연결 오류 등)
- **ERROR**: 사용 안함 (캐시 실패는 에러가 아님)

## 성능 최적화 가이드

### 1. 캐시 키 최적화

- 키 길이 최소화 (메모리 절약)
- 상품명은 최대 50자로 제한
- 특수문자 제거로 키 충돌 방지

### 2. 직렬화 최적화

- Jackson ObjectMapper 사용
- 불필요한 필드 제외 (`@JsonIgnore`)
- 순환 참조 방지

### 3. 패턴 매칭 최적화

- `keys` 명령은 블로킹 - 프로덕션에서 주의
- 가능하면 정확한 키로 삭제
- 대안: Redis Scan 명령 사용 고려

## 트러블슈팅

### 문제: 캐시 히트율이 낮음

**원인**:
- TTL이 너무 짧음
- 캐시 키가 자주 변경됨 (정렬, 페이징 조건)
- 무효화가 너무 자주 발생

**해결**:
- TTL 증가 검토
- 자주 사용되는 조건만 캐시
- 무효화 전략 재검토

### 문제: 메모리 사용량이 높음

**원인**:
- TTL이 너무 김
- 캐시 키가 너무 많음
- 데이터 크기가 큼

**해결**:
- TTL 감소
- 캐시 대상 축소 (인기 상품만)
- 데이터 압축 또는 필드 제거

### 문제: 데이터 정합성 문제

**원인**:
- 무효화 로직 누락
- TTL이 너무 김
- 무효화 실패

**해결**:
- 무효화 로직 추가
- TTL 감소
- 무효화 실패 로그 모니터링

## 향후 개선 사항

1. **캐시 워밍업**
   - 서버 시작 시 인기 상품 미리 캐시
   - 스케줄러로 주기적 갱신

2. **캐시 계층화**
   - Local Cache (Caffeine) + Redis
   - 2단계 캐시로 성능 향상

3. **캐시 압축**
   - 큰 데이터는 압축 저장
   - 메모리 사용량 감소

4. **캐시 통계 대시보드**
   - Grafana 대시보드 구축
   - 실시간 히트율 모니터링

## 참고 자료

- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Jackson Documentation](https://github.com/FasterXML/jackson-docs)
