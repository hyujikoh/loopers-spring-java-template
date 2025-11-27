package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;
import com.loopers.infrastructure.cache.CacheKeyGenerator;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 서비스 구현체
 * Hot/Warm/Cold 전략별로 Redis 캐싱 관리
 * 캐시 실패 시 로깅만 하고 서비스는 계속 동작
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final ObjectMapper objectMapper;

    // TTL 상수 - Hot/Warm/Cold 전략별 차별화
    private static final long HOT_DATA_TTL = 60; // Hot: 60분 (배치 갱신)
    private static final long WARM_DATA_TTL = 10; // Warm: 10분 (Cache-Aside)
    private static final long COLD_DATA_TTL = 5; // Cold: 5분 (레거시)
    private static final TimeUnit TTL_UNIT = TimeUnit.MINUTES;

    // ========== Hot: 상품 상세 (배치 갱신, TTL 60분) ==========


    public void cacheProductDetail(Long productId, ProductDetailInfo productDetail) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = objectMapper.writeValueAsString(productDetail);

            redisTemplate.opsForValue().set(key, value, HOT_DATA_TTL, TTL_UNIT);

            log.debug("상품 상세 캐시 저장 - productId: {}", productId);
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 저장 실패 (JSON 직렬화 오류) - productId: {}, error: {}",
                    productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 저장 실패 - productId: {}, error: {}",
                    productId, e.getMessage());
        }
    }


    public void batchCacheProductDetails(java.util.List<ProductDetailInfo> productDetails) {
        if (productDetails == null || productDetails.isEmpty()) {
            log.debug("배치 캐시 저장 대상 없음");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (ProductDetailInfo productDetail : productDetails) {
            try {
                cacheProductDetail(productDetail.id(), productDetail);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("배치 캐시 저장 실패 - productId: {}", productDetail.id());
            }
        }

        log.info("배치 캐시 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }


    public Optional<ProductDetailInfo> getProductDetailFromCache(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - productId: {}", productId);
                return Optional.empty();
            }

            ProductDetailInfo productDetail = objectMapper.readValue(value, ProductDetailInfo.class);
            log.debug("캐시 히트 - productId: {}", productId);

            return Optional.of(productDetail);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - productId: {}", productId);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - productId: {}", productId);
            return Optional.empty();
        }
    }


    public void evictProductDetail(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            redisTemplate.delete(key);

            log.debug("캐시 삭제 - productId: {}", productId);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - productId: {}", productId);
        }
    }


    public void updateProductDetailLikeCount(Long productId, Long likeCount) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 없음 - 좋아요 수 업데이트 스킵 - productId: {}", productId);
                return;
            }

            // 기존 캐시 데이터 역직렬화
            ProductDetailInfo cachedDetail = objectMapper.readValue(value, ProductDetailInfo.class);

            // 좋아요 수만 업데이트한 새 객체 생성
            ProductDetailInfo updatedDetail = new ProductDetailInfo(
                    cachedDetail.id(),
                    cachedDetail.name(),
                    cachedDetail.description(),
                    likeCount, // 새로운 좋아요 수
                    cachedDetail.stockQuantity(),
                    cachedDetail.price(),
                    cachedDetail.brand(),
                    cachedDetail.isLiked()
            );

            // 업데이트된 데이터를 캐시에 저장 (기존 TTL 유지)
            Long ttl = redisTemplate.getExpire(key, TTL_UNIT);
            if (ttl != null && ttl > 0) {
                String updatedValue = objectMapper.writeValueAsString(updatedDetail);
                redisTemplate.opsForValue().set(key, updatedValue, ttl, TTL_UNIT);

                log.debug("좋아요 수 업데이트 - productId: {}, likeCount: {}", productId, likeCount);
            } else {
                log.debug("캐시 TTL 만료 - 업데이트 스킵 - productId: {}", productId);
            }
        } catch (JsonProcessingException e) {
            log.warn("좋아요 수 업데이트 실패 (JSON 처리) - productId: {}", productId);
        } catch (Exception e) {
            log.warn("좋아요 수 업데이트 실패 - productId: {}", productId);
        }
    }

    // ========== Hot/Warm: 상품 ID 리스트 (Hot: 60분, Warm: 10분) ==========


    public void cacheProductIds(CacheStrategy strategy, Long brandId,
                                Pageable pageable,
                                List<Long> productIds) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = objectMapper.writeValueAsString(productIds);

            long ttl = strategy == CacheStrategy.HOT ? HOT_DATA_TTL : WARM_DATA_TTL;
            redisTemplate.opsForValue().set(key, value, ttl, TTL_UNIT);

            log.debug("상품 ID 리스트 캐시 저장 - strategy: {}, brandId: {}", strategy, brandId);
        } catch (JsonProcessingException e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 (JSON 직렬화) - strategy: {}", strategy);
        } catch (Exception e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 - strategy: {}", strategy);
        }
    }


    public Optional<List<Long>> getProductIdsFromCache(CacheStrategy strategy, Long brandId,
                                                       Pageable pageable) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - strategy: {}, brandId: {}", strategy, brandId);
                return Optional.empty();
            }

            List<Long> productIds = objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

            log.debug("캐시 히트 - strategy: {}, brandId: {}", strategy, brandId);

            return Optional.of(productIds);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - strategy: {}", strategy);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - strategy: {}", strategy);
            return Optional.empty();
        }
    }


    public void evictProductIdsByStrategy(CacheStrategy strategy) {
        try {
            String pattern = cacheKeyGenerator.generateProductIdsPattern(strategy);
            deleteByPattern(pattern);

            log.debug("전략별 캐시 삭제 - strategy: {}", strategy);
        } catch (Exception e) {
            log.warn("전략별 캐시 삭제 실패 - strategy: {}", strategy);
        }
    }


    public void evictProductIdsByBrand(CacheStrategy strategy, Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductIdsPatternByBrand(strategy, brandId);
            deleteByPattern(pattern);

            log.debug("브랜드별 캐시 삭제 - strategy: {}, brandId: {}", strategy, brandId);
        } catch (Exception e) {
            log.warn("브랜드별 캐시 삭제 실패 - strategy: {}, brandId: {}", strategy, brandId);
        }
    }

    // ========== Cold: 전체 Page 객체 (레거시, TTL 5분) ==========


    public void cacheProductList(String cacheKey, Page<ProductInfo> productList) {
        try {
            String value = objectMapper.writeValueAsString(productList);

            redisTemplate.opsForValue().set(cacheKey, value, COLD_DATA_TTL, TTL_UNIT);

            log.debug("목록 캐시 저장 - key: {}", cacheKey);
        } catch (JsonProcessingException e) {
            log.warn("목록 캐시 저장 실패 (JSON 직렬화) - key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("목록 캐시 저장 실패 - key: {}", cacheKey);
        }
    }


    public Optional<Page<ProductInfo>> getProductListFromCache(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                log.debug("캐시 미스 - key: {}", cacheKey);
                return Optional.empty();
            }

            // Page 객체 역직렬화
            Page<ProductInfo> productList = objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructParametricType(
                            org.springframework.data.domain.PageImpl.class,
                            ProductInfo.class
                    ));

            log.debug("캐시 히트 - key: {}", cacheKey);

            return Optional.of(productList);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - key: {}", cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - key: {}", cacheKey);
            return Optional.empty();
        }
    }


    public void evictProductListByBrand(Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductListPatternByBrand(brandId);
            deleteByPattern(pattern);

            log.debug("브랜드 목록 캐시 삭제 - brandId: {}", brandId);
        } catch (Exception e) {
            log.warn("브랜드 목록 캐시 삭제 실패 - brandId: {}", brandId);
        }
    }


    public void evictAllProductList() {
        try {
            String pattern = cacheKeyGenerator.generateProductListPattern();
            deleteByPattern(pattern);

            log.debug("전체 목록 캐시 삭제");
        } catch (Exception e) {
            log.warn("전체 목록 캐시 삭제 실패");
        }
    }

    // ========== 범용 캐시 연산 ==========


    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);

            log.debug("캐시 저장 - key: {}", key);
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (JSON 직렬화) - key: {}", key);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - key: {}", key);
        }
    }


    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - key: {}", key);
                return Optional.empty();
            }

            T result = objectMapper.readValue(value, clazz);
            log.debug("캐시 히트 - key: {}", key);

            return Optional.of(result);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - key: {}", key);
            return Optional.empty();
        }
    }


    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("캐시 삭제 - key: {}", key);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - key: {}", key);
        }
    }


    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("패턴 캐시 삭제 - pattern: {}, count: {}", pattern, keys.size());
            } else {
                log.debug("삭제 대상 없음 - pattern: {}", pattern);
            }
        } catch (Exception e) {
            log.warn("패턴 캐시 삭제 실패 - pattern: {}", pattern);
        }
    }

    // ========== 세밀한 캐시 무효화 (Incremental Invalidation) ==========


    public void evictProductCaches(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.debug("무효화 대상 없음");
            return;
        }

        log.info("상품 캐시 무효화 - 대상: {}개", productIds.size());

        int deletedCount = 0;

        for (Long productId : productIds) {
            try {
                evictProductDetail(productId);
                deletedCount++;
            } catch (Exception e) {
                log.warn("상품 캐시 무효화 실패 - productId: {}", productId);
            }
        }

        log.info("상품 캐시 무효화 완료 - 삭제: {}개", deletedCount);
    }


    public void evictBrandCaches(Set<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            log.debug("무효화 대상 없음");
            return;
        }

        log.info("브랜드 캐시 무효화 - 대상: {}개", brandIds.size());

        int deletedKeyCount = 0;

        for (Long brandId : brandIds) {
            try {
                // Hot 캐시 삭제
                String hotPattern = String.format("product:ids:hot:brand:%d:*", brandId);
                Set<String> hotKeys = redisTemplate.keys(hotPattern);
                if (hotKeys != null && !hotKeys.isEmpty()) {
                    redisTemplate.delete(hotKeys);
                    deletedKeyCount += hotKeys.size();
                }

                // Warm 캐시 삭제
                String warmPattern = String.format("product:ids:warm:brand:%d:*", brandId);
                Set<String> warmKeys = redisTemplate.keys(warmPattern);
                if (warmKeys != null && !warmKeys.isEmpty()) {
                    redisTemplate.delete(warmKeys);
                    deletedKeyCount += warmKeys.size();
                }

                // 레거시 캐시 삭제
                evictProductListByBrand(brandId);

            } catch (Exception e) {
                log.warn("브랜드 캐시 무효화 실패 - brandId: {}", brandId);
            }
        }

        log.info("브랜드 캐시 무효화 완료 - 삭제 키: {}개", deletedKeyCount);
    }


    public void evictCachesAfterMVSync(Set<Long> changedProductIds, Set<Long> affectedBrandIds) {
        log.info("MV 동기화 후 캐시 무효화 - 변경상품: {}개, 브랜드: {}개",
                changedProductIds.size(), affectedBrandIds.size());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 변경된 상품 상세 캐시 무효화
            evictProductCaches(changedProductIds);

            // 2. 영향받은 브랜드 목록 캐시 무효화
            evictBrandCaches(affectedBrandIds);

            // 3. 전체 상품 목록 캐시 무효화
            if (!changedProductIds.isEmpty()) {
                deleteByPattern("product:ids:hot:brand:null:*");
                deleteByPattern("product:ids:warm:brand:null:*");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("MV 동기화 캐시 무효화 완료 - {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MV 동기화 캐시 무효화 실패 - {}ms", duration, e);
        }
    }
}
