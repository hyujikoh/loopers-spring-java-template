package com.loopers.infrastructure.cache;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.product.ProductCacheService;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductSearchFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 배치 갱신 스케줄러
 *
 * <p>Hot 데이터를 주기적으로 갱신하여 캐시 스탬피드를 방지합니다.</p>
 *
 * <p>갱신 대상:</p>
 * <ul>
 *   <li>인기 상품 상세 정보 (좋아요 수 상위 100개)</li>
 *   <li>브랜드별 인기순 상품 ID 리스트 (첫 3페이지)</li>
 * </ul>
 *
 * <p>실행 주기:</p>
 * <ul>
 *   <li>상품 상세: 매 50분마다 (TTL 60분보다 10분 전)</li>
 *   <li>상품 ID 리스트: 매 50분마다</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheRefreshScheduler {

    private final ProductService productService;
    private final BrandService brandService;
    private final ProductCacheService productCacheService;

    // 배치 갱신 설정
    private static final int TOP_PRODUCTS_COUNT = 100; // 상위 100개 상품
    private static final int CACHE_PAGES_PER_BRAND = 3; // 브랜드당 3페이지
    private static final int PAGE_SIZE = 20; // 페이지당 20개

    /**
     * Hot 데이터 배치 갱신 - 매 50분마다 실행
     *
     * <p>TTL이 60분이므로 만료 10분 전에 갱신하여 캐시 스탬피드 방지</p>
     */
    @Scheduled(fixedRate = 50 * 60 * 1000, initialDelay = 60 * 1000) // 50분마다, 1분 후 시작
    public void refreshHotDataCache() {
        log.info("Hot 데이터 배치 갱신 시작");

        long startTime = System.currentTimeMillis();

        try {
            // 1. 인기 상품 상세 정보 갱신
            refreshPopularProductDetails();

            // 2. 브랜드별 인기순 상품 ID 리스트 갱신
            refreshBrandPopularProductIds();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Hot 데이터 배치 갱신 완료 - 소요시간: {}ms", duration);

        } catch (Exception e) {
            log.error("Hot 데이터 배치 갱신 실패", e);
        }
    }

    /**
     * 인기 상품 상세 정보 갱신
     *
     * <p>좋아요 수 상위 100개 상품의 상세 정보를 캐시에 저장합니다.</p>
     */
    private void refreshPopularProductDetails() {
        log.debug("인기 상품 상세 정보 갱신 시작");

        try {
            // 좋아요 수 상위 100개 상품 조회
            Pageable pageable = PageRequest.of(0, TOP_PRODUCTS_COUNT,
                    Sort.by(Sort.Direction.DESC, "likeCount"));

            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
            List<ProductEntity> popularProducts = productService.getProducts(filter).getContent();

            // 상품 상세 정보 생성 및 배치 캐싱
            List<ProductDetailInfo> productDetails = popularProducts.stream()
                    .map(product -> {
                        try {
                            BrandEntity brand = brandService.getBrandById(product.getBrandId());
                            return ProductDetailInfo.of(product, brand, false);
                        } catch (Exception e) {
                            log.warn("상품 상세 정보 생성 실패 - productId: {}", product.getId(), e);
                            return null;
                        }
                    })
                    .filter(detail -> detail != null)
                    .collect(Collectors.toList());

            productCacheService.batchCacheProductDetails(productDetails);

            log.info("인기 상품 상세 정보 갱신 완료 - 대상: {}개, 성공: {}개",
                    popularProducts.size(), productDetails.size());

        } catch (Exception e) {
            log.error("인기 상품 상세 정보 갱신 실패", e);
        }
    }

    /**
     * 브랜드별 인기순 상품 ID 리스트 갱신
     *
     * <p>각 브랜드의 인기순 상품 ID 리스트 첫 3페이지를 캐시에 저장합니다.</p>
     */
    private void refreshBrandPopularProductIds() {
        log.debug("브랜드별 인기순 상품 ID 리스트 갱신 시작");

        try {
            // 모든 활성 브랜드 조회
            List<BrandEntity> brands = brandService.getAllBrands();

            int totalRefreshed = 0;

            for (BrandEntity brand : brands) {
                try {
                    int refreshed = refreshBrandProductIds(brand.getId());
                    totalRefreshed += refreshed;
                } catch (Exception e) {
                    log.warn("브랜드 상품 ID 리스트 갱신 실패 - brandId: {}", brand.getId(), e);
                }
            }

            log.info("브랜드별 인기순 상품 ID 리스트 갱신 완료 - 브랜드 수: {}, 갱신된 페이지: {}개",
                    brands.size(), totalRefreshed);

        } catch (Exception e) {
            log.error("브랜드별 인기순 상품 ID 리스트 갱신 실패", e);
        }
    }

    /**
     * 특정 브랜드의 상품 ID 리스트 갱신
     *
     * @param brandId 브랜드 ID
     * @return 갱신된 페이지 수
     */
    private int refreshBrandProductIds(Long brandId) {
        int refreshedPages = 0;

        // 인기순 정렬
        Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");

        // 첫 3페이지 갱신
        for (int page = 0; page < CACHE_PAGES_PER_BRAND; page++) {
            try {
                Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
                ProductSearchFilter filter = new ProductSearchFilter(brandId, null, pageable);

                List<ProductEntity> products = productService.getProducts(filter).getContent();

                if (products.isEmpty()) {
                    break; // 더 이상 상품이 없으면 중단
                }

                // ID 리스트 추출 및 캐싱
                List<Long> productIds = products.stream()
                        .map(ProductEntity::getId)
                        .collect(Collectors.toList());

                productCacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);

                refreshedPages++;

                log.debug("브랜드 상품 ID 리스트 갱신 - brandId: {}, page: {}, size: {}",
                        brandId, page, productIds.size());

            } catch (Exception e) {
                log.warn("브랜드 상품 ID 리스트 갱신 실패 - brandId: {}, page: {}", brandId, page, e);
            }
        }

        return refreshedPages;
    }
}
