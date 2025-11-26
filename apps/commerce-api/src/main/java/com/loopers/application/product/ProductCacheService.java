package com.loopers.application.product;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;

/**
 * 상품 캐시 서비스 인터페이스
 *
 */
public interface ProductCacheService {
    
    /**
     * 상품 상세 정보를 캐시에 저장
     * 
     * @param productId 상품 ID
     * @param productDetail 상품 상세 정보
     */
    void cacheProductDetail(Long productId, ProductDetailInfo productDetail);
    
    /**
     * 상품 상세 정보를 캐시에서 조회
     * 
     * @param productId 상품 ID
     * @return 캐시된 상품 상세 정보 (없으면 empty)
     */
    Optional<ProductDetailInfo> getProductDetailFromCache(Long productId);
    
    /**
     * 상품 상세 캐시 삭제
     * 
     * @param productId 상품 ID
     */
    void evictProductDetail(Long productId);
    
    /**
     * 상품 목록을 캐시에 저장
     * 
     * @param cacheKey 캐시 키
     * @param productList 상품 목록
     */
    void cacheProductList(String cacheKey, Page<ProductInfo> productList);
    
    /**
     * 상품 목록을 캐시에서 조회
     * 
     * @param cacheKey 캐시 키
     * @return 캐시된 상품 목록 (없으면 empty)
     */
    Optional<Page<ProductInfo>> getProductListFromCache(String cacheKey);
    
    /**
     * 특정 브랜드의 모든 상품 목록 캐시 삭제
     * 
     * @param brandId 브랜드 ID
     */
    void evictProductListByBrand(Long brandId);
    
    /**
     * 모든 상품 목록 캐시 삭제
     */
    void evictAllProductList();
    
    /**
     * 캐시 저장 (범용)
     * 
     * @param key 캐시 키
     * @param value 저장할 값
     * @param timeout TTL
     * @param timeUnit 시간 단위
     */
    void set(String key, Object value, long timeout, TimeUnit timeUnit);
    
    /**
     * 캐시 조회 (범용)
     * 
     * @param key 캐시 키
     * @param clazz 반환 타입
     * @return 캐시된 값 (없으면 empty)
     */
    <T> Optional<T> get(String key, Class<T> clazz);
    
    /**
     * 캐시 삭제 (범용)
     * 
     * @param key 캐시 키
     */
    void delete(String key);
    
    /**
     * 패턴 매칭으로 캐시 삭제
     * 
     * @param pattern 캐시 키 패턴 (예: "product:list:*")
     */
    void deleteByPattern(String pattern);
}
