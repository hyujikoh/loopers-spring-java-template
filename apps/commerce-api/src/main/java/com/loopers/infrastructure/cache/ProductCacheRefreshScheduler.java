package com.loopers.infrastructure.cache;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductSearchFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 배치 갱신 스케줄러
 * <p>
 * Hot 데이터를 주기적으로 갱신하여 캐시 스탬피드 방지
 * - 인기 상품 상세 정보 (좋아요 수 상위 100개)
 * - 브랜드별 인기순 상품 ID 리스트 (첫 3페이지)
 * - 실행 주기: 50분마다 (TTL 60분보다 10분 전에 갱신)
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
     * Hot 데이터 배치 갱신 (50분마다)
     * TTL 60분보다 10분 전에 갱신하여 스탬피드 방지
     */
    @Scheduled(fixedRate = 50 * 60 * 1000, initialDelay = 60 * 1000) // 50분마다, 1분 후 시작
    public void refreshHotDataCache() {
        log.info("Hot 데이터 배치 갱신 시작");

        long startTime = System.currentTimeMillis();

        try {
            refreshPopularProductDetails();
            refreshBrandPopularProductIds();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Hot 데이터 배치 갱신 완료 - {}ms", duration);

        } catch (Exception e) {
            log.error("Hot 데이터 배치 갱신 실패", e);
        }
    }

    /**
     * 인기 상품 상세 정보 갱신 (좋아요 수 상위 100개)
     */
    private void refreshPopularProductDetails() {
        log.debug("인기 상품 상세 정보 갱신");

        try {
            Pageable pageable = PageRequest.of(0, TOP_PRODUCTS_COUNT,
                    Sort.by(Sort.Direction.DESC, "likeCount"));

            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
            List<ProductEntity> popularProducts = productService.getProducts(filter).getContent();

            List<ProductDetailInfo> productDetails = popularProducts.stream()
                    .map(product -> {
                        try {
                            BrandEntity brand = brandService.getBrandById(product.getBrandId());
                            return ProductDetailInfo.of(product, brand, false);
                        } catch (Exception e) {
                            log.warn("상품 상세 생성 실패 - productId: {}", product.getId());
                            return null;
                        }
                    })
                    .filter(detail -> detail != null)
                    .collect(Collectors.toList());

            productCacheService.batchCacheProductDetails(productDetails);

            log.info("인기 상품 상세 갱신 완료 - {}개", productDetails.size());

        } catch (Exception e) {
            log.error("인기 상품 상세 갱신 실패", e);
        }
    }

    /**
     * 브랜드별 인기순 상품 ID 리스트 갱신 (첫 3페이지)
     */
    private void refreshBrandPopularProductIds() {
        log.debug("브랜드별 인기순 ID 리스트 갱신");

        try {
            List<BrandEntity> brands = brandService.getAllBrands();

            int totalRefreshed = 0;

            for (BrandEntity brand : brands) {
                try {
                    int refreshed = refreshBrandProductIds(brand.getId());
                    totalRefreshed += refreshed;
                } catch (Exception e) {
                    log.warn("브랜드 ID 리스트 갱신 실패 - brandId: {}", brand.getId());
                }
            }

            log.info("브랜드별 ID 리스트 갱신 완료 - 브랜드: {}개, 갱신: {}개",
                    brands.size(), totalRefreshed);

        } catch (Exception e) {
            log.error("브랜드별 ID 리스트 갱신 실패", e);
        }
    }

    /**
     * 특정 브랜드의 상품 ID 리스트 갱신 (첫 3페이지)
     */
    private int refreshBrandProductIds(Long brandId) {
        int refreshedPages = 0;
        Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");

        for (int page = 0; page < CACHE_PAGES_PER_BRAND; page++) {
            try {
                Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
                ProductSearchFilter filter = new ProductSearchFilter(brandId, null, pageable);

                List<ProductEntity> products = productService.getProducts(filter).getContent();

                if (products.isEmpty()) {
                    break;
                }

                List<Long> productIds = products.stream()
                        .map(ProductEntity::getId)
                        .collect(Collectors.toList());

                productCacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);
                refreshedPages++;

                log.debug("브랜드 ID 리스트 갱신 - brandId: {}, page: {}", brandId, page);

            } catch (Exception e) {
                log.warn("브랜드 ID 리스트 갱신 실패 - brandId: {}, page: {}", brandId, page);
            }
        }

        return refreshedPages;
    }
}
