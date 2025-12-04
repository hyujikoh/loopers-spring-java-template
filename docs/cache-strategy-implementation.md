# Hot/Warm/Cold 캐시 전략 구현 가이드

## 📋 목차
1. [개요](#개요)
2. [캐시 전략 설계](#캐시-전략-설계)
3. [구현 단계](#구현-단계)
4. [핵심 구현 내용](#핵심-구현-내용)
5. [성능 최적화 포인트](#성능-최적화-포인트)
6. [트러블슈팅](#트러블슈팅)

---

## 개요

### 배경
E-commerce 상품 조회 시스템에서 다양한 접근 패턴을 가진 데이터를 효율적으로 캐싱하기 위해 Hot/Warm/Cold 전략을 도입했습니다.

### 목표
- **캐시 스탬피드 방지**: 인기 상품의 동시 다발적 조회 시 DB 부하 방지
- **메모리 효율성**: 자주 조회되는 데이터만 선별적으로 캐싱
- **유연한 TTL 관리**: 데이터 특성에 맞는 차별화된 만료 시간 적용

### 기대 효과
- DB 부하 70% 감소
- 평균 응답 시간 50% 개선
- 캐시 히트율 85% 이상 달성

---

## 캐시 전략 설계

### 전략 분류 기준

| 전략 | 데이터 특성 | 조회 빈도 | TTL | 갱신 방식 |
|------|------------|----------|-----|----------|
| **Hot** | 인기 상품, 브랜드별 인기순 | 매우 높음 | 60분 | 배치 갱신 |
| **Warm** | 브랜드별 일반 목록 | 보통 | 10분 | Cache-Aside |
| **Cold** | 상품명 검색, 복잡한 필터 | 낮음 | 5분 또는 미사용 | 직접 조회 |

### 캐싱 구조 설계

```
┌─────────────────────────────────────────────────────────┐
│                    Redis Cache Layer                     │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  Hot Cache (TTL: 60분)                                   │
│  ├─ product:detail:{id}        → ProductDetailInfo      │
│  └─ product:ids:hot:{key}      → List<Long>             │
│                                                           │
│  Warm Cache (TTL: 10분)                                  │
│  └─ product:ids:warm:{key}     → List<Long>             │
│                                                           │
│  Cold Cache (TTL: 5분 또는 미사용)                        │
│  └─ 직접 DB 조회                                          │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 핵심 설계 원칙

#### 1. ID 리스트 캐싱 패턴
**문제**: 전체 상품 정보를 캐싱하면 개별 상품 변경 시 전체 캐시 무효화 필요

**해결**: ID 리스트만 캐싱하고 개별 상품은 별도 캐시 활용
```java
// 캐시 구조
product:ids:hot:brand:123:page:0:size:20 → [1, 2, 3, 4, 5, ...]
product:detail:1 → ProductDetailInfo
product:detail:2 → ProductDetailInfo
```

**장점**:
- 개별 상품 변경 시 해당 상품 캐시만 무효화
- 목록 캐시는 유지되어 효율적
- 메모리 사용량 최소화

#### 2. 배치 갱신 시스템
**문제**: TTL 만료 시 동시 다발적 조회로 캐시 스탬피드 발생

**해결**: TTL 만료 전 미리 캐시 갱신
```java
@Scheduled(fixedRate = 50 * 60 * 1000) // 50분마다
public void refreshHotDataCache() {
    // TTL 60분보다 10분 전에 갱신
    refreshPopularProductDetails();
    refreshBrandPopularProductIds();
}
```

**장점**:
- 캐시가 비는 순간 없음
- 안정적인 응답 시간 보장
- DB 부하 예측 가능

---

## 구현 단계

### 1단계: 캐시 키 생성 전략 구현

#### CacheStrategy Enum 정의
```java
public enum CacheStrategy {
    HOT("hot", 60),      // 60분 TTL
    WARM("warm", 10),    // 10분 TTL
    COLD("cold", 5);     // 5분 TTL
    
    private final String prefix;
    private final long ttlMinutes;
}
```

#### CacheKeyGenerator 구현
```java
@Component
public class CacheKeyGenerator {
    
    // 상품 상세 캐시 키
    public String generateProductDetailKey(Long productId) {
        return String.format("product:detail:%d", productId);
    }
    
    // ID 리스트 캐시 키 (전략별)
    public String generateProductIdsKey(
        CacheStrategy strategy, 
        Long brandId, 
        Pageable pageable
    ) {
        StringBuilder keyBuilder = new StringBuilder("product:ids:");
        keyBuilder.append(strategy.getPrefix()).append(":");
        
        if (brandId != null) {
            keyBuilder.append("brand:").append(brandId).append(":");
        }
        
        keyBuilder.append("page:").append(pageable.getPageNumber());
        keyBuilder.append(":size:").append(pageable.getPageSize());
        
        // 정렬 조건 포함
        if (pageable.getSort().isSorted()) {
            String sortKey = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));
            keyBuilder.append(":sort:").append(sortKey);
        }
        
        return keyBuilder.toString();
    }
}
```

**핵심 포인트**:
- 전략별 prefix로 구분 (`hot`, `warm`, `cold`)
- 페이징 정보 포함 (page, size)
- 정렬 조건 포함 (property, direction)
- 브랜드 필터 조건 포함

---

### 2단계: 캐시 서비스 인터페이스 설계

#### ProductCacheService 인터페이스
```java
public interface ProductCacheService {
    
    // 상품 상세 캐싱 (Hot)
    void cacheProductDetail(Long productId, ProductDetailInfo detail);
    Optional<ProductDetailInfo> getProductDetailFromCache(Long productId);
    void evictProductDetail(Long productId);
    
    // ID 리스트 캐싱 (Hot/Warm/Cold)
    void cacheProductIds(
        CacheStrategy strategy, 
        Long brandId, 
        Pageable pageable, 
        List<Long> productIds
    );
    
    Optional<List<Long>> getProductIdsFromCache(
        CacheStrategy strategy, 
        Long brandId, 
        Pageable pageable
    );
    
    // 배치 갱신
    void batchCacheProductDetails(List<ProductDetailInfo> details);
    
    // 캐시 무효화
    void evictProductIdsByBrand(CacheStrategy strategy, Long brandId);
    void evictProductIdsByStrategy(CacheStrategy strategy);
}
```

**설계 원칙**:
- 전략별 메서드 분리로 명확한 의도 표현
- 배치 처리 메서드 제공
- 세밀한 캐시 무효화 지원

---

### 3단계: 캐시 서비스 구현

#### ProductCacheServiceImpl 핵심 구현
```java
@Service
@RequiredArgsConstructor
public class ProductCacheServiceImpl implements ProductCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    
    @Override
    public void cacheProductDetail(Long productId, ProductDetailInfo detail) {
        String key = cacheKeyGenerator.generateProductDetailKey(productId);
        long ttl = CacheStrategy.HOT.getTtlMinutes();
        
        redisTemplate.opsForValue().set(key, detail, ttl, TimeUnit.MINUTES);
        
        log.debug("상품 상세 캐시 저장 - productId: {}, TTL: {}분", productId, ttl);
    }
    
    @Override
    public void cacheProductIds(
        CacheStrategy strategy, 
        Long brandId, 
        Pageable pageable, 
        List<Long> productIds
    ) {
        String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
        long ttl = strategy.getTtlMinutes();
        
        redisTemplate.opsForValue().set(key, productIds, ttl, TimeUnit.MINUTES);
        
        log.debug("상품 ID 리스트 캐시 저장 - strategy: {}, brandId: {}, page: {}, TTL: {}분",
                strategy, brandId, pageable.getPageNumber(), ttl);
    }
    
    @Override
    public void batchCacheProductDetails(List<ProductDetailInfo> details) {
        if (details.isEmpty()) {
            return;
        }
        
        long ttl = CacheStrategy.HOT.getTtlMinutes();
        
        details.forEach(detail -> {
            String key = cacheKeyGenerator.generateProductDetailKey(detail.id());
            redisTemplate.opsForValue().set(key, detail, ttl, TimeUnit.MINUTES);
        });
        
        log.info("상품 상세 배치 캐싱 완료 - 개수: {}, TTL: {}분", details.size(), ttl);
    }
}
```

**구현 포인트**:
- 전략별 TTL 자동 적용
- 배치 처리로 성능 최적화
- 상세한 로깅으로 디버깅 지원

---

### 4단계: 배치 갱신 스케줄러 구현

#### ProductCacheRefreshScheduler
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheRefreshScheduler {
    
    private final ProductService productService;
    private final BrandService brandService;
    private final ProductCacheService productCacheService;
    
    private static final int TOP_PRODUCTS_COUNT = 100;
    private static final int CACHE_PAGES_PER_BRAND = 3;
    private static final int PAGE_SIZE = 20;
    
    @Scheduled(fixedRate = 50 * 60 * 1000, initialDelay = 60 * 1000)
    public void refreshHotDataCache() {
        log.info("Hot 데이터 배치 갱신 시작");
        
        long startTime = System.currentTimeMillis();
        
        try {
            refreshPopularProductDetails();
            refreshBrandPopularProductIds();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Hot 데이터 배치 갱신 완료 - 소요시간: {}ms", duration);
            
        } catch (Exception e) {
            log.error("Hot 데이터 배치 갱신 실패", e);
        }
    }
    
    private void refreshPopularProductDetails() {
        // 좋아요 수 상위 100개 상품 조회
        Pageable pageable = PageRequest.of(0, TOP_PRODUCTS_COUNT, 
            Sort.by(Sort.Direction.DESC, "likeCount"));
        
        ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
        List<ProductEntity> popularProducts = productService.getProducts(filter)
            .getContent();
        
        // 상품 상세 정보 생성 및 배치 캐싱
        List<ProductDetailInfo> productDetails = popularProducts.stream()
            .map(product -> {
                BrandEntity brand = brandService.getBrandById(product.getBrandId());
                return ProductDetailInfo.of(product, brand, false);
            })
            .collect(Collectors.toList());
        
        productCacheService.batchCacheProductDetails(productDetails);
        
        log.info("인기 상품 상세 정보 갱신 완료 - 대상: {}개", productDetails.size());
    }
    
    private void refreshBrandPopularProductIds() {
        List<BrandEntity> brands = brandService.getAllBrands();
        
        int totalRefreshed = 0;
        
        for (BrandEntity brand : brands) {
            int refreshed = refreshBrandProductIds(brand.getId());
            totalRefreshed += refreshed;
        }
        
        log.info("브랜드별 인기순 상품 ID 리스트 갱신 완료 - 브랜드 수: {}, 갱신된 페이지: {}개", 
                brands.size(), totalRefreshed);
    }
    
    private int refreshBrandProductIds(Long brandId) {
        int refreshedPages = 0;
        Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");
        
        for (int page = 0; page < CACHE_PAGES_PER_BRAND; page++) {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
            ProductSearchFilter filter = new ProductSearchFilter(brandId, null, pageable);
            
            List<ProductEntity> products = productService.getProducts(filter)
                .getContent();
            
            if (products.isEmpty()) {
                break;
            }
            
            List<Long> productIds = products.stream()
                .map(ProductEntity::getId)
                .collect(Collectors.toList());
            
            productCacheService.cacheProductIds(
                CacheStrategy.HOT, brandId, pageable, productIds
            );
            
            refreshedPages++;
        }
        
        return refreshedPages;
    }
}
```

**스케줄링 전략**:
- **실행 주기**: 50분마다 (TTL 60분보다 10분 전)
- **초기 지연**: 1분 (애플리케이션 시작 후 안정화 대기)
- **갱신 대상**:
  - 인기 상품 상위 100개
  - 각 브랜드별 인기순 첫 3페이지

---

### 5단계: Facade 계층 통합

#### ProductFacade 캐시 전략 적용
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {
    
    private final ProductService productService;
    private final ProductCacheService productCacheService;
    
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter filter) {
        CacheStrategy strategy = determineCacheStrategy(filter);
        
        switch (strategy) {
            case HOT:
                return getProductsWithHotCache(filter);
            case WARM:
                return getProductsWithWarmCache(filter);
            case COLD:
            default:
                return getProductsWithoutCache(filter);
        }
    }
    
    private CacheStrategy determineCacheStrategy(ProductSearchFilter filter) {
        // 상품명 검색이 있으면 Cold
        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            return CacheStrategy.COLD;
        }
        
        // 브랜드 필터 + 인기순 정렬 = Hot
        if (filter.brandId() != null && isPopularitySort(filter.pageable())) {
            return CacheStrategy.HOT;
        }
        
        // 브랜드 필터만 = Warm
        if (filter.brandId() != null) {
            return CacheStrategy.WARM;
        }
        
        // 기본 = Warm
        return CacheStrategy.WARM;
    }
    
    private boolean isPopularitySort(Pageable pageable) {
        return pageable.getSort().stream()
            .anyMatch(order -> "likeCount".equals(order.getProperty()) 
                            && order.isDescending());
    }
    
    private Page<ProductInfo> getProductsWithHotCache(ProductSearchFilter filter) {
        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
            CacheStrategy.HOT, filter.brandId(), filter.pageable()
        );
        
        if (cachedIds.isPresent()) {
            log.debug("Hot 캐시 히트 - brandId: {}", filter.brandId());
            return buildPageFromIds(cachedIds.get(), filter.pageable());
        }
        
        // 캐시 미스 - DB 조회 후 캐싱
        Page<ProductEntity> products = productService.getProducts(filter);
        
        List<Long> productIds = products.getContent().stream()
            .map(ProductEntity::getId)
            .collect(Collectors.toList());
        
        productCacheService.cacheProductIds(
            CacheStrategy.HOT, filter.brandId(), filter.pageable(), productIds
        );
        
        return products.map(ProductInfo::of);
    }
    
    private Page<ProductInfo> buildPageFromIds(List<Long> productIds, Pageable pageable) {
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        List<ProductInfo> products = new ArrayList<>();
        for (Long productId : productIds) {
            ProductEntity product = productService.getProductDetail(productId);
            products.add(ProductInfo::of(product));
        }
        
        return new PageImpl<>(products, pageable, products.size());
    }
}
```

**전략 선택 로직**:
1. 상품명 검색 → Cold (캐시 미사용)
2. 브랜드 + 인기순 → Hot (배치 갱신)
3. 브랜드만 → Warm (Cache-Aside)
4. 기타 → Warm

---

## 핵심 구현 내용

### 1. 캐시 키 설계

#### 상품 상세 캐시 키
```
product:detail:{productId}
예: product:detail:123
```

#### ID 리스트 캐시 키
```
product:ids:{strategy}:brand:{brandId}:page:{page}:size:{size}:sort:{sort}
예: product:ids:hot:brand:5:page:0:size:20:sort:likeCount:DESC
```

**설계 원칙**:
- 계층적 구조로 관리 용이
- 전략별 prefix로 명확한 구분
- 모든 조회 조건 포함으로 정확한 캐시 매칭

### 2. TTL 차별화 전략

| 전략 | TTL | 이유 |
|------|-----|------|
| Hot | 60분 | 배치 갱신으로 스탬피드 방지, 긴 TTL 가능 |
| Warm | 10분 | Cache-Aside 패턴, 적절한 신선도 유지 |
| Cold | 5분 | 낮은 조회 빈도, 짧은 TTL로 메모리 효율화 |

### 3. 캐시 무효화 전략

#### 개별 상품 변경 시
```java
public void evictProductCaches(Long productId, Long brandId) {
    // 1. 상품 상세 캐시 삭제
    productCacheService.evictProductDetail(productId);
    
    // 2. 해당 브랜드의 ID 리스트 캐시 삭제
    productCacheService.evictProductIdsByBrand(CacheStrategy.HOT, brandId);
    productCacheService.evictProductIdsByBrand(CacheStrategy.WARM, brandId);
}
```

**장점**:
- 변경된 상품만 영향
- 다른 브랜드 캐시는 유지
- 최소한의 캐시 무효화

---

## 성능 최적화 포인트

### 1. 배치 갱신 최적화

#### 병렬 처리 고려사항
```java
// 현재: 순차 처리
for (BrandEntity brand : brands) {
    refreshBrandProductIds(brand.getId());
}

// 개선: 병렬 처리 (향후 적용 가능)
brands.parallelStream()
    .forEach(brand -> refreshBrandProductIds(brand.getId()));
```

### 2. 메모리 사용량 최적화

#### ID 리스트 캐싱의 메모리 효율
```
전체 상품 정보 캐싱:
- 20개 상품 × 2KB = 40KB per page
- 100 페이지 = 4MB

ID 리스트 캐싱:
- 20개 ID × 8 bytes = 160 bytes per page
- 100 페이지 = 16KB

메모리 절감: 99.6%
```

### 3. 캐시 히트율 모니터링

#### 로깅 전략
```java
@Aspect
@Component
public class CacheMonitoringAspect {
    
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    @Around("execution(* ProductCacheService.get*(..))")
    public Object monitorCacheAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        
        if (result instanceof Optional) {
            Optional<?> optional = (Optional<?>) result;
            if (optional.isPresent()) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }
        
        return result;
    }
    
    @Scheduled(fixedRate = 60000)
    public void logCacheStatistics() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        
        if (total > 0) {
            double hitRate = (double) hits / total * 100;
            log.info("캐시 히트율: {:.2f}% (히트: {}, 미스: {})", 
                    hitRate, hits, misses);
        }
    }
}
```

---

## 트러블슈팅

### 문제 1: 캐시 스탬피드 발생

**증상**: TTL 만료 시 동시 다발적 DB 조회로 응답 시간 급증

**원인**: Cache-Aside 패턴의 한계

**해결**:
```java
// Before: Cache-Aside만 사용
Optional<List<Long>> cached = getFromCache(key);
if (cached.isEmpty()) {
    List<Long> data = loadFromDB();
    saveToCache(key, data);
}

// After: Hot 데이터는 배치 갱신
@Scheduled(fixedRate = 50 * 60 * 1000)
public void refreshHotDataCache() {
    // TTL 만료 전 미리 갱신
}
```

### 문제 2: 개별 상품 변경 시 전체 캐시 무효화

**증상**: 한 상품 변경 시 모든 목록 캐시 무효화로 캐시 효율 저하

**원인**: 전체 상품 정보를 캐싱하는 구조

**해결**:
```java
// Before: 전체 상품 정보 캐싱
cache.put("products:list", List<ProductInfo>);

// After: ID 리스트만 캐싱
cache.put("products:ids", List<Long>);
cache.put("product:detail:1", ProductDetailInfo);
```

### 문제 3: 메모리 부족

**증상**: Redis 메모리 사용량 급증

**원인**: 모든 조회 조건을 캐싱

**해결**:
```java
// Cold 데이터는 캐시 미사용
private CacheStrategy determineCacheStrategy(ProductSearchFilter filter) {
    if (filter.productName() != null) {
        return CacheStrategy.COLD; // 캐시 미사용
    }
    // ...
}
```

---

## 결론

### 달성한 성과
- ✅ 캐시 스탬피드 완전 방지 (Hot 데이터)
- ✅ 메모리 사용량 99% 절감 (ID 리스트 캐싱)
- ✅ 유연한 TTL 관리 (전략별 차별화)
- ✅ 세밀한 캐시 무효화 (브랜드별, 전략별)

### 향후 개선 방향
1. **병렬 처리 도입**: 배치 갱신 시 브랜드별 병렬 처리
2. **캐시 워밍**: 애플리케이션 시작 시 Hot 데이터 미리 로드
3. **동적 TTL 조정**: 조회 빈도에 따른 TTL 자동 조정
4. **캐시 압축**: 대용량 데이터 압축 저장

### 참고 자료
- [Redis 캐싱 전략](https://redis.io/docs/manual/patterns/)
- [Cache Stampede 방지 기법](https://en.wikipedia.org/wiki/Cache_stampede)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)

---

**작성일**: 2025-01-26  
**작성자**: Loopers 개발팀  
**버전**: 1.0
