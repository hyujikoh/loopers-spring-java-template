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
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.*;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 관련 비즈니스 로직을 처리하는 퍼사드 클래스
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
    private final BrandService brandService;

    /**
     * 상품 목록 조회
     *
     * @param productSearchFilter 검색 조건
     * @return 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        CacheStrategy strategy = productCacheService.determineCacheStrategy(productSearchFilter);

        return switch (strategy) {
            case HOT -> getProductsWithHotCache(productSearchFilter);
            case WARM -> getProductsWithWarmCache(productSearchFilter);
            default -> getProductsWithoutCache(productSearchFilter);
        };
    }

    /**
     * Hot 전략: ID 리스트 캐싱 + 배치 갱신
     */
    private Page<ProductInfo> getProductsWithHotCache(ProductSearchFilter filter) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();

        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
                CacheStrategy.HOT, brandId, pageable
        );

        if (cachedIds.isPresent()) {
            log.debug("Hot 캐시 히트 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
            return buildPageFromMVIds(cachedIds.get(), pageable);
        }

        log.debug("Hot 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());

        Page<ProductMaterializedViewEntity> mvProducts;
        if (brandId != null) {
            mvProducts = mvService.findByBrandId(brandId, pageable);
        } else {
            mvProducts = mvService.findAll(pageable);
        }

        List<Long> productIds = mvProducts.getContent().stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .collect(Collectors.toList());

        productCacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);

        return mvProducts.map(ProductInfo::from);
    }

    /**
     * Warm 전략: ID 리스트 캐싱 + Cache-Aside
     */
    private Page<ProductInfo> getProductsWithWarmCache(ProductSearchFilter filter) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();

        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
                CacheStrategy.WARM, brandId, pageable
        );

        if (cachedIds.isPresent()) {
            log.debug("Warm 캐시 히트 - brandId: {}, page: {}", brandId, pageable.getPageNumber());
            return buildPageFromMVIds(cachedIds.get(), pageable);
        }

        log.debug("Warm 캐시 미스 - brandId: {}, page: {}", brandId, pageable.getPageNumber());

        Page<ProductMaterializedViewEntity> mvProducts;
        if (brandId != null) {
            mvProducts = mvService.findByBrandId(brandId, pageable);
        } else {
            mvProducts = mvService.findAll(pageable);
        }

        List<Long> productIds = mvProducts.getContent().stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .collect(Collectors.toList());

        productCacheService.cacheProductIds(CacheStrategy.WARM, brandId, pageable, productIds);

        return mvProducts.map(ProductInfo::from);
    }

    /**
     * Cold 전략: 캐시 미사용
     */
    private Page<ProductInfo> getProductsWithoutCache(ProductSearchFilter filter) {
        log.debug("Cold 전략 - 캐시 미사용, MV 테이블 직접 조회");

        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            Page<ProductMaterializedViewEntity> mvProducts =
                    mvService.searchByName(filter.productName(), filter.pageable());
            return mvProducts.map(ProductInfo::from);
        }

        Page<ProductMaterializedViewEntity> mvProducts;
        if (filter.brandId() != null) {
            mvProducts = mvService.findByBrandId(filter.brandId(), filter.pageable());
        } else {
            mvProducts = mvService.findAll(filter.pageable());
        }

        return mvProducts.map(ProductInfo::from);
    }

    /**
     * MV ID 리스트로부터 Page 객체 구성
     */
    private Page<ProductInfo> buildPageFromMVIds(List<Long> productIds, Pageable pageable) {
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ProductMaterializedViewEntity> mvEntities = mvService.findByIds(productIds);

        List<Long> foundMVIds = mvEntities.stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .toList();

        List<ProductInfo> products = mvEntities.stream()
                .map(ProductInfo::from)
                .collect(Collectors.toList());

        List<Long> missingIds = productIds.stream()
                .filter(id -> !foundMVIds.contains(id))
                .collect(Collectors.toList());

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

        return new PageImpl<>(products, pageable, products.size());
    }

    /**
     * 상품 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String username) {
        Optional<ProductDetailInfo> cachedDetail = productCacheService.getProductDetailFromCache(productId);

        ProductDetailInfo productDetail;

        if (cachedDetail.isPresent()) {
            log.debug("상품 상세 캐시 히트 - productId: {}", productId);
            productDetail = cachedDetail.get();
        } else {
            log.debug("상품 상세 캐시 미스 - productId: {}", productId);

            Optional<ProductMaterializedViewEntity> mvEntityOpt = mvService.findById(productId);

            if (mvEntityOpt.isPresent()) {
                productDetail = ProductDetailInfo.from(mvEntityOpt.get(), false);
            } else {
                log.warn("MV에 없는 상품 - 폴백 조회 시작 - productId: {}", productId);

                ProductEntity product = productService.getActiveProductDetail(productId);
                BrandEntity brand = brandService.getBrandById(product.getBrandId());
                Long likeCount = likeService.countByProduct(product);

                productDetail = ProductDetailInfo.of(product, brand, likeCount, false);

                log.info("폴백 조회 완료 - productId: {} (배치 동기화 대기 중)", productId);
            }

            productCacheService.cacheProductDetail(productId, productDetail);
        }

        if (username != null) {
            Boolean isLiked = likeService.findLike(
                            userService.getUserByUsername(username).getId(),
                            productId
                    )
                    .map(like -> like.getDeletedAt() == null)
                    .orElse(false);

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
