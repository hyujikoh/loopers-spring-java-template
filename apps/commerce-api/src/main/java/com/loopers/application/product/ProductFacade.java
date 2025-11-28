package com.loopers.application.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
 * 상품 관련 유스케이스를 조정하는 Application Facade
 * Application Layer의 역할:
 *   도메인 서비스 호출 및 조정
 *   도메인 엔티티를 DTO로 변환
 *   트랜잭션 경계 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {
    private final ProductService productService;
    private final ProductMVService mvService;
    private final ProductCacheService productCacheService;
    private final LikeService likeService;
    private final BrandService brandService;
    private final UserService userService;

    /**
     *
     * 도메인 서비스에서 MV 엔티티를 조회하고, Facade에서 DTO로 변환합니다.
     *
     * @param productSearchFilter 검색 조건
     * @return 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        // 1. 캐시 전략 결정
        CacheStrategy strategy = productCacheService.determineCacheStrategy(productSearchFilter);

        // 2. 도메인 서비스에서 MV 엔티티 조회
        Page<ProductMaterializedViewEntity> mvEntities =
                productService.getMVEntitiesByStrategy(productSearchFilter, strategy);

        // 4. DTO 변환
        return mvEntities.map(ProductInfo::from);
    }

    /**
     *
     * 도메인 서비스에서 엔티티를 조회하고, Facade에서 DTO로 변환합니다.
     *
     * @param productId 상품 ID
     * @param username 사용자명 (nullable)
     * @return 상품 상세 정보
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String username) {
        // 1. 캐시 조회
        Optional<ProductDetailInfo> cachedDetail = productCacheService.getProductDetailFromCache(productId);

        if (cachedDetail.isPresent()) {
            log.debug("상품 상세 캐시 히트 - productId: {}", productId);
            return applyUserLikeStatus(cachedDetail.get(), username);
        }

        log.debug("상품 상세 캐시 미스 - productId: {}", productId);

        // 2. MV 엔티티 조회
        ProductMaterializedViewEntity productMaterializedViewEntity = mvService.getById(productId);

        ProductDetailInfo  productDetail = ProductDetailInfo.from(productMaterializedViewEntity, false);

        // 4. 캐시 저장
        productCacheService.cacheProductDetail(productId, productDetail);

        // 5. 사용자 좋아요 상태 적용
        return applyUserLikeStatus(productDetail, username);
    }

    /**
     * MV에 없는 상품을 폴백 조회하여 DTO로 변환합니다.
     */
    private ProductDetailInfo getProductDetailWithFallback(Long productId) {
        log.warn("MV에 없는 상품 - 폴백 조회 시작 - productId: {}", productId);

        // 도메인 엔티티 조회
        ProductEntity product = productService.getActiveProductDetail(productId);
        BrandEntity brand = brandService.getBrandById(product.getBrandId());
        Long likeCount = likeService.countByProduct(product);

        // 도메인 엔티티 → DTO 변환
        ProductDetailInfo productDetail = ProductDetailInfo.of(product, brand, likeCount, false);

        log.info("폴백 조회 완료 - productId: {} (배치 동기화 대기 중)", productId);

        return productDetail;
    }

    /**
     * 사용자 좋아요 상태를 DTO에 적용합니다.
     */
    private ProductDetailInfo applyUserLikeStatus(ProductDetailInfo productDetail, String username) {
        if (username == null) {
            return productDetail;
        }

        // 사용자 좋아요 상태 조회
        Long userId = userService.getUserByUsername(username).getId();
        Boolean isLiked = likeService.findLike(userId, productDetail.id())
                .map(like -> like.getDeletedAt() == null)
                .orElse(false);

        // DTO 재구성
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

    /**
     * 상품을 삭제합니다.
     *
     * 상품 삭제 후 MV 동기화 및 캐시 무효화를 수행합니다.
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void deletedProduct(Long productId) {
        // 1. 상품 삭제
        ProductEntity product = productService.getActiveProductDetail(productId);
        product.delete();

        // 2. MV 동기화
        mvService.syncProduct(productId);

        // 3. 캐시 무효화
        productCacheService.getProductDetailFromCache(productId)
                .ifPresent(detail -> productCacheService.evictProductDetail(productId));
    }
}
