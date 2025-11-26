package com.loopers.application.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 Facade
 * 
 * <p>Hot/Warm/Cold 캐시 전략을 적용한 상품 조회 서비스</p>
 * 
 * <p>캐시 전략:</p>
 * <ul>
 *   <li>Hot: 상품 상세 - 배치 갱신, 60분 TTL</li>
 *   <li>Warm: 브랜드별 목록 - Cache-Aside, 10분 TTL</li>
 *   <li>Cold: 복잡한 검색 - 캐시 미사용 또는 짧은 TTL</li>
 * </ul>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;
    private final LikeService likeService;
    private final UserService userService;
    private final ProductCacheService productCacheService;
    private final com.loopers.domain.like.ProductLikeStatsService statsService;

    /**
     * 상품 목록 조회 (Hot/Warm/Cold 전략 적용)
     * 
     * <p>전략 선택 기준:</p>
     * <ul>
     *   <li>Hot: 브랜드별 인기순 정렬 (배치 갱신 대상)</li>
     *   <li>Warm: 브랜드별 단순 조회 (Cache-Aside)</li>
     *   <li>Cold: 상품명 검색 등 복잡한 조건 (캐시 미사용)</li>
     * </ul>
     * 
     * @param productSearchFilter 검색 조건
     * @return 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        CacheStrategy strategy = determineCacheStrategy(productSearchFilter);

        return switch (strategy) {
            case HOT -> getProductsWithHotCache(productSearchFilter);
            case WARM -> getProductsWithWarmCache(productSearchFilter);
            default -> getProductsWithoutCache(productSearchFilter);
        };
    }
    
    /**
     * 캐시 전략 결정
     * 
     * <p>결정 기준:</p>
     * <ul>
     *   <li>Hot: 브랜드 필터 + 인기순 정렬 + 상품명 검색 없음</li>
     *   <li>Warm: 브랜드 필터 + 상품명 검색 없음</li>
     *   <li>Cold: 상품명 검색 있음 또는 복잡한 조건</li>
     * </ul>
     */
    private CacheStrategy determineCacheStrategy(ProductSearchFilter filter) {
        // 상품명 검색이 있으면 Cold (캐시 미사용)
        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            log.debug("Cold 전략 선택 - 상품명 검색: {}", filter.productName());
            return CacheStrategy.COLD;
        }
        
        // 브랜드 필터가 있는 경우
        if (filter.brandId() != null) {
            // 인기순 정렬이면 Hot (배치 갱신 대상)
            if (isPopularitySort(filter.pageable())) {
                log.debug("Hot 전략 선택 - 브랜드: {}, 인기순 정렬", filter.brandId());
                return CacheStrategy.HOT;
            }
            // 그 외 정렬은 Warm (Cache-Aside)
            log.debug("Warm 전략 선택 - 브랜드: {}", filter.brandId());
            return CacheStrategy.WARM;
        }
        
        // 전체 목록 조회는 Warm
        log.debug("Warm 전략 선택 - 전체 목록");
        return CacheStrategy.WARM;
    }
    
    /**
     * 인기순 정렬 여부 확인
     */
    private boolean isPopularitySort(Pageable pageable) {
        return pageable.getSort().stream()
            .anyMatch(order -> "likeCount".equals(order.getProperty()) && order.isDescending());
    }
    
    /**
     * Hot 전략: ID 리스트 캐싱 + 배치 갱신
     * 
     * <p>배치 갱신 시스템에서 미리 캐시를 갱신하므로 캐시 스탬피드 방지</p>
     */
    private Page<ProductInfo> getProductsWithHotCache(ProductSearchFilter filter) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();
        
        // 1. ID 리스트 캐시 조회
        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
            CacheStrategy.HOT, brandId, pageable
        );
        
        if (cachedIds.isPresent()) {
            log.debug("Hot 캐시 히트 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
            return buildPageFromIds(cachedIds.get(), pageable);
        }
        
        // 2. 캐시 미스 - DB 조회 후 캐시 저장
        log.debug("Hot 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
        Page<ProductEntity> products = productService.getProducts(filter);
        
        // 3. ID 리스트 캐싱
        List<Long> productIds = products.getContent().stream()
            .map(ProductEntity::getId)
            .collect(Collectors.toList());
        
        productCacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);
        
        return products.map(ProductInfo::of);
    }
    
    /**
     * Warm 전략: ID 리스트 캐싱 + Cache-Aside
     * 
     * <p>일반적인 Cache-Aside 패턴 적용</p>
     */
    private Page<ProductInfo> getProductsWithWarmCache(ProductSearchFilter filter) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();
        
        // 1. ID 리스트 캐시 조회
        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
            CacheStrategy.WARM, brandId, pageable
        );
        
        if (cachedIds.isPresent()) {
            log.debug("Warm 캐시 히트 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
            return buildPageFromIds(cachedIds.get(), pageable);
        }
        
        // 2. 캐시 미스 - DB 조회 후 캐시 저장
        log.debug("Warm 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
        Page<ProductEntity> products = productService.getProducts(filter);
        
        // 3. ID 리스트 캐싱
        List<Long> productIds = products.getContent().stream()
            .map(ProductEntity::getId)
            .collect(Collectors.toList());
        
        productCacheService.cacheProductIds(CacheStrategy.WARM, brandId, pageable, productIds);
        
        return products.map(ProductInfo::of);
    }
    
    /**
     * Cold 전략: 캐시 미사용
     * 
     * <p>복잡한 검색 조건은 캐시 효율이 낮으므로 DB 직접 조회</p>
     */
    private Page<ProductInfo> getProductsWithoutCache(ProductSearchFilter filter) {
        log.debug("Cold 전략 - 캐시 미사용, 직접 DB 조회");
        Page<ProductEntity> products = productService.getProducts(filter);
        return products.map(ProductInfo::of);
    }
    
    /**
     * ID 리스트로부터 Page 객체 구성
     * 
     * <p>캐시된 ID 리스트를 사용하여 개별 상품 정보를 조회합니다.</p>
     * <p>개별 상품 정보는 Hot 캐시(상품 상세)에서 조회됩니다.</p>
     */
    private Page<ProductInfo> buildPageFromIds(List<Long> productIds, Pageable pageable) {
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // 좋아요 수 배치 조회 (N+1 방지)
        var likeCountMap = statsService.getLikeCountMap(productIds);
        
        // 개별 상품 정보 조회 (Hot 캐시 활용)
        List<ProductInfo> products = new ArrayList<>();
        for (Long productId : productIds) {
            try {
                ProductEntity product = productService.getProductDetail(productId);
                Long likeCount = likeCountMap.getOrDefault(productId, 0L);
                products.add(ProductInfo.of(product, likeCount));
            } catch (Exception e) {
                log.warn("상품 조회 실패 - productId: {}, error: {}", productId, e.getMessage());
            }
        }
        
        // Page 객체 구성 (totalElements는 실제 조회된 개수)
        return new PageImpl<>(products, pageable, products.size());
    }


    /**
     * 상품 상세 정보 조회 (Hot 전략 - 배치 갱신)
     * 
     * <p>상품 상세는 Hot 데이터로 분류되어 배치 갱신 시스템에서 미리 캐시를 갱신합니다.</p>
     * <p>TTL: 60분, 캐시 스탬피드 방지</p>
     *
     * @param productId 상품 ID
     * @param username  사용자명 (null인 경우 비로그인 사용자)
     * @return 상품 상세 정보 (좋아요 여부 포함)
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String username) {
        // 1. 캐시에서 상품 상세 조회 시도
        Optional<ProductDetailInfo> cachedDetail = productCacheService.getProductDetailFromCache(productId);
        
        ProductDetailInfo productDetail;
        
        if (cachedDetail.isPresent()) {
            log.debug("상품 상세 캐시 히트 - productId: {}", productId);
            productDetail = cachedDetail.get();
        } else {
            // 2. 캐시 미스 - DB 조회
            log.debug("상품 상세 캐시 미스 - productId: {}", productId);
            
            ProductEntity product = productService.getProductDetail(productId);
            BrandEntity brand = brandService.getBrandById(product.getBrandId());
            
            // 3. MV 테이블에서 좋아요 수 조회
            Long likeCount = statsService.getLikeCount(productId);
            
            // 좋아요 여부는 캐시하지 않음 (사용자별로 다름)
            productDetail = ProductDetailInfo.of(product, brand, likeCount, false);
            
            // 4. 캐시 저장
            productCacheService.cacheProductDetail(productId, productDetail);
        }
        
        // 5. 사용자별 좋아요 여부 확인 (캐시 불가)
        if (username != null) {
            Boolean isLiked = likeService.findLike(
                userService.getUserByUsername(username).getId(), 
                productId
            )
            .map(like -> like.getDeletedAt() == null)
            .orElse(false);
            
            // 좋아요 정보를 포함한 새로운 객체 생성
            return new ProductDetailInfo(
                productDetail.id(),
                productDetail.name(),
                productDetail.description(),
                productDetail.likeCount(),
                productDetail.stockQuantity(),
                productDetail.price(),
                productDetail.brand(),
                isLiked
            );
        }
        
        return productDetail;
    }
    
    /**
     * 상품 정보 변경 시 캐시 무효화
     * 
     * <p>상품 정보가 변경되면 관련 캐시를 모두 무효화합니다.</p>
     * 
     * @param productId 변경된 상품 ID
     * @param brandId 상품의 브랜드 ID
     */
    public void evictProductCaches(Long productId, Long brandId) {
        log.info("상품 캐시 무효화 시작 - productId: {}, brandId: {}", productId, brandId);
        
        // 1. 상품 상세 캐시 삭제 (Hot)
        productCacheService.evictProductDetail(productId);
        
        // 2. 해당 브랜드의 ID 리스트 캐시 삭제 (Hot/Warm)
        productCacheService.evictProductIdsByBrand(CacheStrategy.HOT, brandId);
        productCacheService.evictProductIdsByBrand(CacheStrategy.WARM, brandId);
        
        log.info("상품 캐시 무효화 완료 - productId: {}, brandId: {}", productId, brandId);
    }
    
    /**
     * 전체 상품 목록 캐시 무효화
     * 
     * <p>대량 데이터 변경 시 사용합니다.</p>
     */
    public void evictAllProductListCaches() {
        log.info("전체 상품 목록 캐시 무효화 시작");
        
        productCacheService.evictProductIdsByStrategy(CacheStrategy.HOT);
        productCacheService.evictProductIdsByStrategy(CacheStrategy.WARM);
        productCacheService.evictAllProductList(); // 레거시
        
        log.info("전체 상품 목록 캐시 무효화 완료");
    }
}
