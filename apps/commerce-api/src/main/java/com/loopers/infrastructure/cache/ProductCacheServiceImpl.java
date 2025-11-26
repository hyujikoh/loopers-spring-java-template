package com.loopers.infrastructure.cache;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
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
    
    // TTL 상수
    private static final long PRODUCT_DETAIL_TTL = 30; // 30분
    private static final long PRODUCT_LIST_TTL = 5; // 5분
    private static final TimeUnit TTL_UNIT = TimeUnit.MINUTES;
    
    @Override
    public void cacheProductDetail(Long productId, ProductDetailInfo productDetail) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = objectMapper.writeValueAsString(productDetail);
            
            redisTemplate.opsForValue().set(key, value, PRODUCT_DETAIL_TTL, TTL_UNIT);
            
            log.debug("상품 상세 캐시 저장 성공 - productId: {}, key: {}", productId, key);
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 저장 실패 (JSON 직렬화 오류) - productId: {}, error: {}", 
                    productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 저장 실패 - productId: {}, error: {}", 
                    productId, e.getMessage());
        }
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
    public void cacheProductList(String cacheKey, Page<ProductInfo> productList) {
        try {
            String value = objectMapper.writeValueAsString(productList);
            
            redisTemplate.opsForValue().set(cacheKey, value, PRODUCT_LIST_TTL, TTL_UNIT);
            
            log.debug("상품 목록 캐시 저장 성공 - key: {}, size: {}", 
                    cacheKey, productList.getContent().size());
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
                log.debug("상품 목록 캐시 미스 - key: {}", cacheKey);
                return Optional.empty();
            }
            
            // Page 객체 역직렬화
            Page<ProductInfo> productList = objectMapper.readValue(value, 
                objectMapper.getTypeFactory().constructParametricType(
                    org.springframework.data.domain.PageImpl.class, 
                    ProductInfo.class
                ));
            
            log.debug("상품 목록 캐시 히트 - key: {}, size: {}", 
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
            
            log.debug("전체 상품 목록 캐시 삭제 성공 - pattern: {}", pattern);
        } catch (Exception e) {
            log.warn("전체 상품 목록 캐시 삭제 실패 - error: {}", e.getMessage());
        }
    }
    
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
