package com.loopers.infrastructure.cache;

import java.util.StringJoiner;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성기
 *
 * <p>Hot/Warm/Cold 데이터 전략에 따른 캐시 키를 생성합니다.</p>
 *
 * <p>캐시 키 패턴:</p>
 * <ul>
 *   <li>상품 상세 (Hot): product:detail:{productId}</li>
 *   <li>상품 ID 리스트 (Hot/Warm): product:ids:{strategy}:{brandId}:{page}:{size}:{sort}</li>
 *   <li>레거시 목록 (Cold): product:page:{brandId}:{productName}:{page}:{size}:{sort}</li>
 * </ul>
 *
 * <p>전략별 캐시 키 특징:</p>
 * <ul>
 *   <li>Hot: 배치 갱신 대상, 긴 TTL (60분)</li>
 *   <li>Warm: Cache-Aside 패턴, 중간 TTL (10분)</li>
 *   <li>Cold: 캐시 미사용 또는 짧은 TTL</li>
 * </ul>
 */
@Component
public class CacheKeyGenerator {

    private static final String DELIMITER = ":";
    private static final String NULL_VALUE = "null";

    // 캐시 키 프리픽스
    private static final String PRODUCT_PREFIX = "product";
    private static final String DETAIL_PREFIX = "detail";
    private static final String IDS_PREFIX = "ids";
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
     * 상품 ID 리스트 캐시 키 생성 (Hot/Warm 전략)
     *
     * <p>패턴: product:ids:{strategy}:{brandId}:{page}:{size}:{sort}</p>
     *
     * <p>ID 리스트만 캐싱하여 개별 상품 정보 변경 시 전체 캐시 무효화를 방지합니다.</p>
     *
     * @param strategy 캐시 전략 (HOT, WARM)
     * @param brandId  브랜드 ID (nullable)
     * @param pageable 페이징 정보
     * @return 캐시 키
     */
    public String generateProductIdsKey(CacheStrategy strategy, Long brandId, Pageable pageable) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(IDS_PREFIX)
                .add(strategy.name().toLowerCase())
                .add(brandId != null ? String.valueOf(brandId) : NULL_VALUE)
                .add(String.valueOf(pageable.getPageNumber()))
                .add(String.valueOf(pageable.getPageSize()))
                .add(generateSortString(pageable.getSort()))
                .toString();
    }

    /**
     * 상품 목록 캐시 키 생성 (레거시 - Cold 전략)
     *
     * <p>패턴: product:page:{brandId}:{productName}:{page}:{size}:{sort}</p>
     *
     * <p>검색 조건이 복잡한 경우 사용하는 레거시 방식입니다.</p>
     *
     * @param brandId     브랜드 ID (nullable)
     * @param productName 상품명 (nullable)
     * @param pageable    페이징 정보
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
     * 특정 전략의 상품 ID 리스트 캐시 키 패턴 생성
     *
     * <p>패턴: product:ids:{strategy}:*</p>
     *
     * @param strategy 캐시 전략
     * @return 캐시 키 패턴
     */
    public String generateProductIdsPattern(CacheStrategy strategy) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(IDS_PREFIX)
                .add(strategy.name().toLowerCase())
                .add("*")
                .toString();
    }

    /**
     * 특정 브랜드의 상품 ID 리스트 캐시 키 패턴 생성
     *
     * <p>패턴: product:ids:{strategy}:{brandId}:*</p>
     *
     * @param strategy 캐시 전략
     * @param brandId  브랜드 ID
     * @return 캐시 키 패턴
     */
    public String generateProductIdsPatternByBrand(CacheStrategy strategy, Long brandId) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(IDS_PREFIX)
                .add(strategy.name().toLowerCase())
                .add(String.valueOf(brandId))
                .add("*")
                .toString();
    }

    /**
     * 특정 브랜드의 모든 목록 캐시 키 패턴 생성 (레거시)
     *
     * <p>패턴: product:page:{brandId}:*</p>
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
     * 모든 상품 목록 캐시 키 패턴 생성 (레거시)
     *
     * <p>패턴: product:page:*</p>
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
