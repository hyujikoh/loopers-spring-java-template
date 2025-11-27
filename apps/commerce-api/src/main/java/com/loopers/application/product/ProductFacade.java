package com.loopers.application.product;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMaterializedViewEntity;
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
 * <p>Materialized View 테이블을 사용하여 조회 성능 최적화 (조인 제거)</p>
 *
 * <p>캐시 전략:</p>
 * <ul>
 *   <li>Hot: 상품 상세 - 배치 갱신, 60분 TTL</li>
 *   <li>Warm: 브랜드별 목록 - Cache-Aside, 10분 TTL</li>
 *   <li>Cold: 복잡한 검색 - 캐시 미사용 또는 짧은 TTL</li>
 * </ul>
 *
 * <p>데이터 소스 (CQRS 패턴):</p>
 * <ul>
 *   <li>조회(Read): ProductMaterializedViewEntity - 상품, 브랜드, 좋아요 통합 조회 (성능 최적화)</li>
 *   <li>폴백(Fallback): ProductEntity - MV에 없는 신규 상품 조회 (배치 미반영 대응)</li>
 *   <li>쓰기(Write): ProductEntity - 도메인 로직 및 데이터 변경</li>
 * </ul>
 *
 * <p>폴백 전략:</p>
 * <ul>
 *   <li>MV 테이블은 2분 간격 배치로 동기화되므로 신규 상품은 MV에 없을 수 있음</li>
 *   <li>MV 조회 실패 시 ProductEntity + BrandEntity + LikeStats로 폴백 조회</li>
 *   <li>폴백 조회 결과도 캐시에 저장하여 다음 조회 성능 보장</li>
 * </ul>
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {
    private final ProductMVService mvService;
    private final ProductService productService;
    private final LikeService likeService;
    private final UserService userService;
    private final ProductCacheService productCacheService;
    private final com.loopers.domain.brand.BrandService brandService; // 폴백용: 브랜드 정보 조회

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
     * <p>MV 테이블을 사용하여 조인 없이 빠른 조회</p>
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
            return buildPageFromMVIds(cachedIds.get(), pageable);
        }

        // 2. 캐시 미스 - MV 테이블 조회 후 캐시 저장
        log.debug("Hot 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());

        // MV 테이블에서 직접 조회 (조인 없음)
        Page<com.loopers.domain.product.ProductMaterializedViewEntity> mvProducts;
        if (brandId != null) {
            mvProducts = mvService.findByBrandId(brandId, pageable);
        } else {
            mvProducts = mvService.findAll(pageable);
        }

        // 3. ID 리스트 캐싱
        List<Long> productIds = mvProducts.getContent().stream()
                .map(com.loopers.domain.product.ProductMaterializedViewEntity::getProductId)
                .collect(Collectors.toList());

        productCacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);

        // 4. MV 엔티티를 ProductInfo로 변환
        return mvProducts.map(this::convertMVToProductInfo);
    }

    /**
     * Warm 전략: ID 리스트 캐싱 + Cache-Aside
     *
     * <p>일반적인 Cache-Aside 패턴 적용</p>
     * <p>MV 테이블을 사용하여 조인 없이 빠른 조회</p>
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
            return buildPageFromMVIds(cachedIds.get(), pageable);
        }

        // 2. 캐시 미스 - MV 테이블 조회 후 캐시 저장
        log.debug("Warm 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());

        // MV 테이블에서 직접 조회 (조인 없음)
        Page<com.loopers.domain.product.ProductMaterializedViewEntity> mvProducts;
        if (brandId != null) {
            mvProducts = mvService.findByBrandId(brandId, pageable);
        } else {
            mvProducts = mvService.findAll(pageable);
        }

        // 3. ID 리스트 캐싱
        List<Long> productIds = mvProducts.getContent().stream()
                .map(com.loopers.domain.product.ProductMaterializedViewEntity::getProductId)
                .collect(Collectors.toList());

        productCacheService.cacheProductIds(CacheStrategy.WARM, brandId, pageable, productIds);

        // 4. MV 엔티티를 ProductInfo로 변환
        return mvProducts.map(this::convertMVToProductInfo);
    }

    /**
     * Cold 전략: 캐시 미사용
     *
     * <p>복잡한 검색 조건은 캐시 효율이 낮으므로 MV 테이블 직접 조회</p>
     */
    private Page<ProductInfo> getProductsWithoutCache(ProductSearchFilter filter) {
        log.debug("Cold 전략 - 캐시 미사용, MV 테이블 직접 조회");

        // 상품명 검색이 있는 경우 MV 테이블에서 검색
        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            Page<com.loopers.domain.product.ProductMaterializedViewEntity> mvProducts =
                    mvService.searchByName(filter.productName(), filter.pageable());
            return mvProducts.map(this::convertMVToProductInfo);
        }

        // 기타 복잡한 조건은 MV 테이블 조회
        Page<com.loopers.domain.product.ProductMaterializedViewEntity> mvProducts;
        if (filter.brandId() != null) {
            mvProducts = mvService.findByBrandId(filter.brandId(), filter.pageable());
        } else {
            mvProducts = mvService.findAll(filter.pageable());
        }

        return mvProducts.map(this::convertMVToProductInfo);
    }

    /**
     * MV ID 리스트로부터 Page 객체 구성
     *
     * <p>캐시된 ID 리스트를 사용하여 MV 테이블에서 상품 정보를 조회합니다.</p>
     * <p>조인 없이 단일 테이블 조회로 성능 최적화</p>
     * <p>MV에 없는 상품은 ProductEntity에서 폴백 조회</p>
     */
    private Page<ProductInfo> buildPageFromMVIds(List<Long> productIds, Pageable pageable) {
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // MV 테이블에서 일괄 조회 (조인 없음, 좋아요 수 포함)
        List<ProductMaterializedViewEntity> mvEntities = mvService.findByIds(productIds);

        // MV에서 조회된 상품 ID 목록
        List<Long> foundMVIds = mvEntities.stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .toList();

        // MV 엔티티를 ProductInfo로 변환
        List<ProductInfo> products = mvEntities.stream()
                .map(this::convertMVToProductInfo)
                .collect(Collectors.toList());

        // MV에 없는 상품 ID 찾기 (신규 상품 또는 배치 미반영)
        List<Long> missingIds = productIds.stream()
                .filter(id -> !foundMVIds.contains(id))
                .collect(Collectors.toList());

        // 폴백: MV에 없는 상품은 ProductEntity에서 조회
        if (!missingIds.isEmpty()) {
            log.warn("MV에 없는 상품 발견 (배치 미반영) - productIds: {}", missingIds);

            for (Long productId : missingIds) {
                try {
                    ProductEntity product = productService.getActiveProductDetail(productId);
                    Long likeCount = likeService.countByProduct(product);
                    products.add(ProductInfo.of(product, likeCount));
                } catch (Exception e) {
                    log.error("폴백 조회 실패 - productId: {}, error: {}", productId, e.getMessage());
                }
            }
        }

        // Page 객체 구성
        return new PageImpl<>(products, pageable, products.size());
    }

    /**
     * ProductMaterializedViewEntity를 ProductInfo로 변환
     *
     * <p>MV 엔티티는 이미 모든 필요한 정보(상품, 브랜드, 좋아요)를 포함하고 있습니다.</p>
     */
    private ProductInfo convertMVToProductInfo(ProductMaterializedViewEntity mv) {
        return ProductInfo.from(mv);
    }


    /**
     * 상품 상세 정보 조회 (Hot 전략 - 배치 갱신)
     *
     * <p>상품 상세는 Hot 데이터로 분류되어 배치 갱신 시스템에서 미리 캐시를 갱신합니다.</p>
     * <p>MV 테이블을 사용하여 조인 없이 빠른 조회 (상품 + 브랜드 + 좋아요 통합)</p>
     * <p>MV에 없는 경우 ProductEntity에서 폴백 조회 (신규 상품 또는 배치 미반영)</p>
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
            // 2. 캐시 미스 - MV 테이블에서 조회 시도
            log.debug("상품 상세 캐시 미스 - productId: {}", productId);

            Optional<ProductMaterializedViewEntity> mvEntityOpt = mvService.findById(productId);

            if (mvEntityOpt.isPresent()) {
                // MV 테이블에서 조회 성공 (조인 없음)
                productDetail = ProductDetailInfo.from(mvEntityOpt.get(), false);
            } else {
                // 3. 폴백: MV에 없는 경우 ProductEntity에서 조회 (신규 상품 또는 배치 미반영)
                log.warn("MV에 없는 상품 - 폴백 조회 시작 - productId: {}", productId);

                ProductEntity product = productService.getActiveProductDetail(productId);
                BrandEntity brand = brandService.getBrandById(product.getBrandId());
                Long likeCount = likeService.countByProduct(product);

                productDetail = ProductDetailInfo.of(product, brand, likeCount, false);

                log.info("폴백 조회 완료 - productId: {} (배치 동기화 대기 중)", productId);
            }

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

    @Transactional
    public void deletedProduct(Long productId) {
        ProductEntity productDetail = productService.getActiveProductDetail(productId);
        productDetail.delete();
        mvService.syncProduct(productId);
        Optional<ProductDetailInfo> productDetailFromCache = productCacheService.getProductDetailFromCache(productId);
        productDetailFromCache.ifPresent(detail -> productCacheService.evictProductDetail(productId));
    }
}
