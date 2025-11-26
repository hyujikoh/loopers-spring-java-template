package com.loopers.application.product;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.infrastructure.cache.CacheStrategy;

/**
 * 상품 캐시 서비스 인터페이스
 * 
 * <p>Hot/Warm/Cold 데이터 전략에 따른 캐싱을 제공합니다.</p>
 * 
 * <p>전략별 특징:</p>
 * <ul>
 *   <li>Hot: 배치 갱신, 긴 TTL (60분), ID 리스트 캐싱</li>
 *   <li>Warm: Cache-Aside, 중간 TTL (10분), ID 리스트 캐싱</li>
 *   <li>Cold: 캐시 미사용 또는 짧은 TTL</li>
 * </ul>
 */
public interface ProductCacheService {
    
    // ========== Hot 데이터: 상품 상세 (배치 갱신) ==========
    
    /**
     * 상품 상세 정보를 캐시에 저장 (Hot 전략)
     * 
     * <p>TTL: 60분, 배치 갱신 대상</p>
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
     * 여러 상품의 상세 정보를 배치로 캐시에 저장
     * 
     * <p>배치 갱신 시스템에서 사용합니다.</p>
     * 
     * @param productDetails 상품 상세 정보 목록
     */
    void batchCacheProductDetails(List<ProductDetailInfo> productDetails);
    
    // ========== Hot/Warm 데이터: 상품 ID 리스트 ==========
    
    /**
     * 상품 ID 리스트를 캐시에 저장
     * 
     * <p>전체 상품 정보 대신 ID 리스트만 캐싱하여 효율성을 높입니다.</p>
     * 
     * @param strategy 캐시 전략 (HOT, WARM)
     * @param brandId 브랜드 ID (nullable)
     * @param pageable 페이징 정보
     * @param productIds 상품 ID 리스트
     */
    void cacheProductIds(CacheStrategy strategy, Long brandId, Pageable pageable, List<Long> productIds);
    
    /**
     * 상품 ID 리스트를 캐시에서 조회
     * 
     * @param strategy 캐시 전략
     * @param brandId 브랜드 ID (nullable)
     * @param pageable 페이징 정보
     * @return 캐시된 상품 ID 리스트 (없으면 empty)
     */
    Optional<List<Long>> getProductIdsFromCache(CacheStrategy strategy, Long brandId, Pageable pageable);
    
    /**
     * 특정 전략의 모든 상품 ID 리스트 캐시 삭제
     * 
     * @param strategy 캐시 전략
     */
    void evictProductIdsByStrategy(CacheStrategy strategy);
    
    /**
     * 특정 브랜드의 상품 ID 리스트 캐시 삭제
     * 
     * @param strategy 캐시 전략
     * @param brandId 브랜드 ID
     */
    void evictProductIdsByBrand(CacheStrategy strategy, Long brandId);
    
    // ========== 레거시: 전체 Page 객체 캐싱 (Cold 전략) ==========
    
    /**
     * 상품 목록을 캐시에 저장 (레거시)
     * 
     * <p>검색 조건이 복잡한 경우 사용하는 레거시 방식입니다.</p>
     * 
     * @param cacheKey 캐시 키
     * @param productList 상품 목록
     */
    void cacheProductList(String cacheKey, Page<ProductInfo> productList);
    
    /**
     * 상품 목록을 캐시에서 조회 (레거시)
     * 
     * @param cacheKey 캐시 키
     * @return 캐시된 상품 목록 (없으면 empty)
     */
    Optional<Page<ProductInfo>> getProductListFromCache(String cacheKey);
    
    /**
     * 특정 브랜드의 모든 상품 목록 캐시 삭제 (레거시)
     * 
     * @param brandId 브랜드 ID
     */
    void evictProductListByBrand(Long brandId);
    
    /**
     * 모든 상품 목록 캐시 삭제 (레거시)
     */
    void evictAllProductList();
    
    // ========== 범용 캐시 연산 ==========
    
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
     * @param pattern 캐시 키 패턴 (예: "product:ids:hot:*")
     */
    void deleteByPattern(String pattern);
}
