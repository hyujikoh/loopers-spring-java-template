package com.loopers.infrastructure.cache;

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
import com.loopers.application.product.ProductCacheService;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 서비스 구현체
 * 
 * <p>RedisTemplate을 사용하여 상품 데이터를 캐시합니다.</p>
 * 
 * <p>캐시 실패 시 로깅만 하고 예외를 전파하지 않아 서비스 안정성을 보장합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheServiceImpl implements ProductCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final ObjectMapper objectMapper;
    
    // TTL 상수 - Hot/Warm/Cold 전략별 차별화
    private static final long HOT_DATA_TTL = 60; // Hot: 60분 (배치 갱신)
    private static final long WARM_DATA_TTL = 10; // Warm: 10분 (Cache-Aside)
    private static final long COLD_DATA_TTL = 5; // Cold: 5분 (레거시)
    private static final TimeUnit TTL_UNIT = TimeUnit.MINUTES;
    
    // ========== Hot 데이터: 상품 상세 (배치 갱신) ==========
    
    @Override
    public void cacheProductDetail(Long productId, ProductDetailInfo productDetail) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = objectMapper.writeValueAsString(productDetail);
            
            redisTemplate.opsForValue().set(key, value, HOT_DATA_TTL, TTL_UNIT);
            
            log.debug("상품 상세 캐시 저장 성공 (Hot) - productId: {}, TTL: {}분", productId, HOT_DATA_TTL);
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 저장 실패 (JSON 직렬화 오류) - productId: {}, error: {}", 
                    productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 저장 실패 - productId: {}, error: {}", 
                    productId, e.getMessage());
        }
    }
    
    @Override
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
                log.warn("배치 캐시 저장 실패 - productId: {}, error: {}", 
                        productDetail.id(), e.getMessage());
            }
        }
        
        log.info("상품 상세 배치 캐시 저장 완료 - 성공: {}, 실패: {}, 전체: {}", 
                successCount, failCount, productDetails.size());
    }
    
    @Override
    public Optional<ProductDetailInfo> getProductDetailFromCache(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("상품 상세 캐시 미스 - productId: {}", productId);
                return Optional.empty();
            }
            
            ProductDetailInfo productDetail = objectMapper.readValue(value, ProductDetailInfo.class);
            log.debug("상품 상세 캐시 히트 - productId: {}", productId);
            
            return Optional.of(productDetail);
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 조회 실패 (JSON 역직렬화 오류) - productId: {}, error: {}", 
                    productId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상품 상세 캐시 조회 실패 - productId: {}, error: {}", 
                    productId, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void evictProductDetail(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            redisTemplate.delete(key);
            
            log.debug("상품 상세 캐시 삭제 성공 - productId: {}", productId);
        } catch (Exception e) {
            log.warn("상품 상세 캐시 삭제 실패 - productId: {}, error: {}", 
                    productId, e.getMessage());
        }
    }
    
    @Override
    public void updateProductDetailLikeCount(Long productId, Long likeCount) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("상품 상세 캐시 없음 - 좋아요 수 업데이트 스킵 - productId: {}", productId);
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
                
                log.debug("상품 상세 캐시 좋아요 수 업데이트 성공 - productId: {}, likeCount: {}, TTL: {}분", 
                        productId, likeCount, ttl);
            } else {
                log.debug("상품 상세 캐시 TTL 만료 - 업데이트 스킵 - productId: {}", productId);
            }
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 좋아요 수 업데이트 실패 (JSON 처리 오류) - productId: {}, error: {}", 
                    productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 좋아요 수 업데이트 실패 - productId: {}, error: {}", 
                    productId, e.getMessage());
        }
    }
    
    // ========== Hot/Warm 데이터: 상품 ID 리스트 ==========
    
    @Override
    public void cacheProductIds(CacheStrategy strategy, Long brandId, 
                                Pageable pageable,
                                List<Long> productIds) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = objectMapper.writeValueAsString(productIds);
            
            long ttl = strategy == CacheStrategy.HOT ? HOT_DATA_TTL : WARM_DATA_TTL;
            redisTemplate.opsForValue().set(key, value, ttl, TTL_UNIT);
            
            log.debug("상품 ID 리스트 캐시 저장 성공 - strategy: {}, brandId: {}, size: {}, TTL: {}분", 
                    strategy, brandId, productIds.size(), ttl);
        } catch (JsonProcessingException e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 (JSON 직렬화 오류) - strategy: {}, error: {}", 
                    strategy, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 - strategy: {}, error: {}", 
                    strategy, e.getMessage());
        }
    }
    
    @Override
    public Optional<List<Long>> getProductIdsFromCache(CacheStrategy strategy, Long brandId,
                                                       Pageable pageable) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("상품 ID 리스트 캐시 미스 - strategy: {}, brandId: {}", strategy, brandId);
                return Optional.empty();
            }
            
            List<Long> productIds = objectMapper.readValue(value,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
            
            log.debug("상품 ID 리스트 캐시 히트 - strategy: {}, brandId: {}, size: {}", 
                    strategy, brandId, productIds.size());
            
            return Optional.of(productIds);
        } catch (JsonProcessingException e) {
            log.warn("상품 ID 리스트 캐시 조회 실패 (JSON 역직렬화 오류) - strategy: {}, error: {}", 
                    strategy, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상품 ID 리스트 캐시 조회 실패 - strategy: {}, error: {}", 
                    strategy, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void evictProductIdsByStrategy(CacheStrategy strategy) {
        try {
            String pattern = cacheKeyGenerator.generateProductIdsPattern(strategy);
            deleteByPattern(pattern);
            
            log.debug("전략별 상품 ID 리스트 캐시 삭제 성공 - strategy: {}, pattern: {}", 
                    strategy, pattern);
        } catch (Exception e) {
            log.warn("전략별 상품 ID 리스트 캐시 삭제 실패 - strategy: {}, error: {}", 
                    strategy, e.getMessage());
        }
    }
    
    @Override
    public void evictProductIdsByBrand(CacheStrategy strategy, Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductIdsPatternByBrand(strategy, brandId);
            deleteByPattern(pattern);
            
            log.debug("브랜드별 상품 ID 리스트 캐시 삭제 성공 - strategy: {}, brandId: {}, pattern: {}", 
                    strategy, brandId, pattern);
        } catch (Exception e) {
            log.warn("브랜드별 상품 ID 리스트 캐시 삭제 실패 - strategy: {}, brandId: {}, error: {}", 
                    strategy, brandId, e.getMessage());
        }
    }
    
    // ========== 레거시: 전체 Page 객체 캐싱 (Cold 전략) ==========
    
    @Override
    public void cacheProductList(String cacheKey, Page<ProductInfo> productList) {
        try {
            String value = objectMapper.writeValueAsString(productList);
            
            redisTemplate.opsForValue().set(cacheKey, value, COLD_DATA_TTL, TTL_UNIT);
            
            log.debug("상품 목록 캐시 저장 성공 (Cold/레거시) - key: {}, size: {}, TTL: {}분", 
                    cacheKey, productList.getContent().size(), COLD_DATA_TTL);
        } catch (JsonProcessingException e) {
            log.warn("상품 목록 캐시 저장 실패 (JSON 직렬화 오류) - key: {}, error: {}", 
                    cacheKey, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 목록 캐시 저장 실패 - key: {}, error: {}", 
                    cacheKey, e.getMessage());
        }
    }
    
    @Override
    public Optional<Page<ProductInfo>> getProductListFromCache(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            
            if (value == null) {
                log.debug("상품 목록 캐시 미스 (레거시) - key: {}", cacheKey);
                return Optional.empty();
            }
            
            // Page 객체 역직렬화
            Page<ProductInfo> productList = objectMapper.readValue(value, 
                objectMapper.getTypeFactory().constructParametricType(
                    org.springframework.data.domain.PageImpl.class, 
                    ProductInfo.class
                ));
            
            log.debug("상품 목록 캐시 히트 (레거시) - key: {}, size: {}", 
                    cacheKey, productList.getContent().size());
            
            return Optional.of(productList);
        } catch (JsonProcessingException e) {
            log.warn("상품 목록 캐시 조회 실패 (JSON 역직렬화 오류) - key: {}, error: {}", 
                    cacheKey, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상품 목록 캐시 조회 실패 - key: {}, error: {}", 
                    cacheKey, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void evictProductListByBrand(Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductListPatternByBrand(brandId);
            deleteByPattern(pattern);
            
            log.debug("브랜드별 상품 목록 캐시 삭제 성공 - brandId: {}, pattern: {}", 
                    brandId, pattern);
        } catch (Exception e) {
            log.warn("브랜드별 상품 목록 캐시 삭제 실패 - brandId: {}, error: {}", 
                    brandId, e.getMessage());
        }
    }
    
    @Override
    public void evictAllProductList() {
        try {
            String pattern = cacheKeyGenerator.generateProductListPattern();
            deleteByPattern(pattern);
            
            log.debug("전체 상품 목록 캐시 삭제 성공 (레거시) - pattern: {}", pattern);
        } catch (Exception e) {
            log.warn("전체 상품 목록 캐시 삭제 실패 - error: {}", e.getMessage());
        }
    }
    
    // ========== 범용 캐시 연산 ==========
    
    @Override
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);
            
            log.debug("캐시 저장 성공 - key: {}", key);
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (JSON 직렬화 오류) - key: {}, error: {}", 
                    key, e.getMessage());
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - key: {}, error: {}", key, e.getMessage());
        }
    }
    
    @Override
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
            log.warn("캐시 조회 실패 (JSON 역직렬화 오류) - key: {}, error: {}", 
                    key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - key: {}, error: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("캐시 삭제 성공 - key: {}", key);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - key: {}, error: {}", key, e.getMessage());
        }
    }
    
    @Override
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("패턴 매칭 캐시 삭제 성공 - pattern: {}, count: {}", 
                        pattern, keys.size());
            } else {
                log.debug("패턴 매칭 캐시 없음 - pattern: {}", pattern);
            }
        } catch (Exception e) {
            log.warn("패턴 매칭 캐시 삭제 실패 - pattern: {}, error: {}", 
                    pattern, e.getMessage());
        }
    }
}
