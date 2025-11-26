package com.loopers.infrastructure.cache;

import java.util.StringJoiner;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성기
 *
 * <p>서비스에서 사용할 캐시 키 생성 유틸 클래스 입니다..</p>
 */
@Component
public class CacheKeyGenerator {
    
    private static final String DELIMITER = ":";
    private static final String NULL_VALUE = "null";
    
    // 캐시 키 프리픽스
    private static final String PRODUCT_PREFIX = "product";
    private static final String DETAIL_PREFIX = "detail";
    private static final String PAGE_PREFIX = "page";
    
    /**
     * 상품 상세 캐시 키 생성
     * 
     * <p>패턴: product:detail:{productId}</p>
     * 
     * @param productId 상품 ID
     * @return 캐시 키
     */
    public String generateProductDetailKey(Long productId) {
        return new StringJoiner(DELIMITER)
            .add(PRODUCT_PREFIX)
            .add(DETAIL_PREFIX)
            .add(String.valueOf(productId))
            .toString();
    }
    
    /**
     * 상품 목록 캐시 키 생성
     * 
     * <p>패턴: product:list:{brandId}:{productName}:{page}:{size}:{sort}</p>
     * 
     * @param brandId 브랜드 ID (nullable)
     * @param productName 상품명 (nullable)
     * @param pageable 페이징 정보
     * @return 캐시 키
     */
    public String generateProductListKey(Long brandId, String productName, Pageable pageable) {
        StringJoiner joiner = new StringJoiner(DELIMITER)
            .add(PRODUCT_PREFIX)
            .add(PAGE_PREFIX)
            .add(brandId != null ? String.valueOf(brandId) : NULL_VALUE)
            .add(productName != null ? sanitizeProductName(productName) : NULL_VALUE)
            .add(String.valueOf(pageable.getPageNumber()))
            .add(String.valueOf(pageable.getPageSize()))
            .add(generateSortString(pageable.getSort()));
        
        return joiner.toString();
    }
    
    /**
     * 특정 브랜드의 모든 목록 캐시 키 패턴 생성
     * 
     * <p>패턴: product:list:{brandId}:*</p>
     * 
     * @param brandId 브랜드 ID
     * @return 캐시 키 패턴
     */
    public String generateProductListPatternByBrand(Long brandId) {
        return new StringJoiner(DELIMITER)
            .add(PRODUCT_PREFIX)
            .add(PAGE_PREFIX)
            .add(String.valueOf(brandId))
            .add("*")
            .toString();
    }
    
    /**
     * 모든 상품 목록 캐시 키 패턴 생성
     * 
     * <p>패턴: product:list:*</p>
     * 
     * @return 캐시 키 패턴
     */
    public String generateProductListPattern() {
        return new StringJoiner(DELIMITER)
            .add(PRODUCT_PREFIX)
            .add(PAGE_PREFIX)
            .add("*")
            .toString();
    }
    
    /**
     * 상품명을 캐시 키에 안전한 형태로 변환
     * 
     * <p>공백, 특수문자 등을 제거하고 최대 길이를 제한합니다.</p>
     */
    private String sanitizeProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return NULL_VALUE;
        }
        
        // 공백을 언더스코어로 변환하고, 특수문자 제거
        String sanitized = productName.trim()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9가-힣_]", "");
        
        // 최대 50자로 제한
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? NULL_VALUE : sanitized;
    }
    
    /**
     * Sort 객체를 문자열로 변환
     * 
     * <p>예: "likeCount_desc,id_asc"</p>
     */
    private String generateSortString(Sort sort) {
        if (sort.isUnsorted()) {
            return "unsorted";
        }
        
        StringJoiner sortJoiner = new StringJoiner(",");
        sort.forEach(order -> {
            String direction = order.getDirection().isAscending() ? "asc" : "desc";
            sortJoiner.add(order.getProperty() + "_" + direction);
        });
        
        return sortJoiner.toString();
    }
}
