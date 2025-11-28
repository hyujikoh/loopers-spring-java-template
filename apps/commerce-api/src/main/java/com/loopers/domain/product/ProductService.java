package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.infrastructure.cache.CacheStrategy;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;

/**
 * 상품 도메인 서비스
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMVService mvService;
    private final ProductCacheService productCacheService;

    /**
     * 검색 필터 조건으로 상품 목록을 조회합니다.
     *
     * @param searchFilter 검색 필터 (브랜드ID, 정렬 조건 등)
     * @return 검색된 상품 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        return productRepository.getProducts(searchFilter);
    }

    /**
     * 상품 ID로 상품 상세 정보를 조회합니다.
     *
     * @param id 상품 ID
     * @return 조회된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ProductEntity getActiveProductDetail(Long id) {
        return productRepository.findActiveById(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    /**
     * 상품 ID로 상품 상세 정보를 조회합니다.
     *
     * @param id 상품 ID
     * @return 조회된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ProductEntity getProductDetail(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    /**
     * 상품 ID로 상품 상세 정보를 조회합니다.
     *
     * @param id 상품 ID
     * @return 조회된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ProductEntity getProductDetailLock(Long id) {
        return productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    /**
     * 상품을 등록합니다.
     * 
     * 상품 등록은 단일 도메인 작업이므로 도메인 서비스에서 트랜잭션 처리합니다.
     *
     * @param request 상품 생성 요청 정보
     * @return 등록된 상품 엔티티
     */
    @Transactional
    public ProductEntity registerProduct(@Valid ProductDomainCreateRequest request) {
        // 상품 엔티티 생성
        ProductEntity productEntity = ProductEntity.createEntity(request);

        return productRepository.save(productEntity);
    }

    /**
     * 상품 엔티티의 재고를 차감합니다.
     * 
     * 이미 조회된 상품 엔티티의 재고를 차감할 때 사용합니다.
     *
     * @param product  재고를 차감할 상품 엔티티
     * @param quantity 차감할 재고 수량
     * @return 재고가 차감된 상품 엔티티
     */
    @Transactional
    public ProductEntity deductStock(ProductEntity product, int quantity) {
        product.deductStock(quantity);
        return productRepository.save(product);
    }

    /**
     * 상품 재고를 원복합니다.
     * 
     * 주문 취소 시 차감된 재고를 다시 복구합니다.
     *
     * @param productId 상품 ID
     * @param quantity  원복할 재고 수량
     * @return 재고가 원복된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional
    public ProductEntity restoreStock(Long productId, int quantity) {
        // 비관적 락을 사용하여 동시성 제어
        ProductEntity product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다. ID: " + productId));

        product.restoreStock(quantity);
        return productRepository.save(product);
    }


    // ========== MV 엔티티 조회 (캐시 전략 포함) ==========

    /**
     * 캐시 전략에 따라 MV 엔티티 목록을 조회합니다.
     *
     * 도메인 엔티티만 반환하며, DTO 변환은 Facade에서 수행합니다.
     *
     * @param filter 검색 조건
     * @param strategy 캐시 전략
     * @return MV 엔티티 페이지
     */
    @Transactional(readOnly = true)
    public Page<ProductMaterializedViewEntity> getMVEntitiesByStrategy(
            ProductSearchFilter filter,
            CacheStrategy strategy
    ) {
        return switch (strategy) {
            case HOT -> getProductsWithCache(filter, CacheStrategy.HOT);
            case WARM -> getProductsWithCache(filter, CacheStrategy.WARM);
            default -> getMVEntitiesWithoutCache(filter);
        };
    }

    /**
     * 캐시를 사용하여 MV 엔티티를 조회합니다.
     */
    private Page<ProductMaterializedViewEntity> getProductsWithCache(
            ProductSearchFilter filter,
            CacheStrategy strategy
    ) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();

        // 1. 캐시에서 ID 리스트 조회
        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
                strategy, brandId, pageable
        );

        if (cachedIds.isPresent()) {
            log.debug("{} 캐시 히트 - brandId: {}, page: {}", strategy, brandId, pageable.getPageNumber());
            return mvService.findByIdsAsPage(cachedIds.get(), pageable);
        }

        log.debug("{} 캐시 미스 - brandId: {}, page: {}", strategy, brandId, pageable.getPageNumber());

        // 2. MV에서 조회
        Page<ProductMaterializedViewEntity> mvProducts = brandId != null
                ? mvService.findByBrandId(brandId, pageable)
                : mvService.findAll(pageable);

        // 3. ID 리스트 캐싱
        List<Long> productIds = mvProducts.getContent().stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .toList();

        productCacheService.cacheProductIds(strategy, brandId, pageable, productIds);

        return mvProducts;
    }

    /**
     * 캐시 없이 MV 엔티티를 조회합니다.
     */
    private Page<ProductMaterializedViewEntity> getMVEntitiesWithoutCache(ProductSearchFilter filter) {
        log.debug("Cold 전략 - 캐시 미사용, MV 테이블 직접 조회");

        // 상품명 검색
        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            return mvService.searchByName(filter.productName(), filter.pageable());
        }

        // 브랜드별 또는 전체 조회
        return filter.brandId() != null
                ? mvService.findByBrandId(filter.brandId(), filter.pageable())
                : mvService.findAll(filter.pageable());
    }
}
