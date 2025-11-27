package com.loopers.application.product;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMVRepository;
import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 Materialized View 서비스
 * 
 * <p>MV 테이블 조회 및 배치 동기화를 담당하는 서비스입니다.
 * 상품, 브랜드, 좋아요 정보를 통합하여 조회 성능을 최적화합니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductMVService {

    private final ProductMVRepository mvRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final LikeRepository likeRepository;

    /**
     * 상품 ID로 MV를 조회합니다.
     * 
     * @param productId 상품 ID
     * @return 상품 MV (존재하지 않으면 Optional.empty())
     */
    public Optional<ProductMaterializedViewEntity> findById(Long productId) {
        return mvRepository.findById(productId);
    }

    /**
     * 전체 상품 MV를 페이징 조회합니다.
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findAll(Pageable pageable) {
        return mvRepository.findAll(pageable);
    }

    /**
     * 브랜드별 상품 MV를 페이징 조회합니다.
     * 
     * @param brandId 브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        return mvRepository.findByBrandId(brandId, pageable);
    }

    /**
     * 여러 상품 ID로 MV를 일괄 조회합니다.
     * 
     * @param productIds 상품 ID 목록
     * @return 상품 MV 목록
     */
    public List<ProductMaterializedViewEntity> findByIds(List<Long> productIds) {
        return mvRepository.findByIdIn(productIds);
    }

    /**
     * 상품명으로 검색하여 MV를 페이징 조회합니다.
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> searchByName(String keyword, Pageable pageable) {
        return mvRepository.findByNameContaining(keyword, pageable);
    }

    /**
     * MV 테이블을 원본 테이블과 동기화합니다.
     * 배치 스케줄러에서 주기적으로 호출됩니다.
     * 
     * @return 배치 업데이트 결과
     */
    @Transactional
    public BatchUpdateResult syncMaterializedView() {
        long startTime = System.currentTimeMillis();
        int updatedCount = 0;
        int createdCount = 0;

        try {
            log.info("MV 배치 동기화 시작");

            // 1. 모든 활성 상품 조회
            List<ProductEntity> allProducts = productRepository.getProducts(
                    new com.loopers.domain.product.dto.ProductSearchFilter(
                            null, null, 
                            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
                    )
            ).getContent();

            if (allProducts.isEmpty()) {
                log.info("동기화할 상품이 없습니다.");
                return BatchUpdateResult.success(0, 0, System.currentTimeMillis() - startTime);
            }

            // 2. 브랜드 정보 일괄 조회
            Map<Long, BrandEntity> brandMap = brandRepository.findAll().stream()
                    .collect(Collectors.toMap(BrandEntity::getId, brand -> brand));

            // 3. 좋아요 수 집계
            List<Long> productIds = allProducts.stream()
                    .map(ProductEntity::getId)
                    .collect(Collectors.toList());
            
            // 각 상품별 좋아요 수를 집계
            Map<Long, Long> likeCountMap = productIds.stream()
                    .collect(Collectors.toMap(
                            productId -> productId,
                            productId -> likeRepository.countByProductIdAndDeletedAtIsNull(productId)
                    ));

            // 4. 기존 MV 조회
            Map<Long, ProductMaterializedViewEntity> existingMVMap = mvRepository.findByIdIn(productIds).stream()
                    .collect(Collectors.toMap(ProductMaterializedViewEntity::getProductId, mv -> mv));

            // 5. MV 생성 또는 업데이트
            List<ProductMaterializedViewEntity> toSave = new ArrayList<>();

            for (ProductEntity product : allProducts) {
                BrandEntity brand = brandMap.get(product.getBrandId());
                if (brand == null) {
                    log.warn("브랜드를 찾을 수 없습니다. productId={}, brandId={}", 
                            product.getId(), product.getBrandId());
                    continue;
                }

                Long likeCount = likeCountMap.getOrDefault(product.getId(), 0L);
                ProductMaterializedViewEntity existingMV = existingMVMap.get(product.getId());

                if (existingMV == null) {
                    // 신규 생성
                    ProductMaterializedViewEntity newMV = ProductMaterializedViewEntity.from(
                            product, brand, likeCount
                    );
                    toSave.add(newMV);
                    createdCount++;
                } else {
                    // 기존 MV 업데이트
                    existingMV.sync(product, brand, likeCount);
                    toSave.add(existingMV);
                    updatedCount++;
                }
            }

            // 6. 일괄 저장
            if (!toSave.isEmpty()) {
                mvRepository.saveAll(toSave);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("MV 배치 동기화 완료 - 생성: {}건, 갱신: {}건, 소요: {}ms", 
                    createdCount, updatedCount, duration);

            return BatchUpdateResult.success(createdCount, updatedCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MV 배치 동기화 실패 - 소요: {}ms", duration, e);
            return BatchUpdateResult.failure(e.getMessage(), duration);
        }
    }

    /**
     * 특정 상품의 MV를 동기화합니다.
     * 
     * @param productId 상품 ID
     */
    @Transactional
    public void syncProduct(Long productId) {
        log.debug("상품 MV 동기화 시작: productId={}", productId);

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("상품을 찾을 수 없습니다. productId=%d", productId)
                ));

        BrandEntity brand = brandRepository.getBrandById(product.getBrandId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("브랜드를 찾을 수 없습니다. brandId=%d", product.getBrandId())
                ));

        Long likeCount = likeRepository.countByProductIdAndDeletedAtIsNull(productId);

        Optional<ProductMaterializedViewEntity> existingMV = mvRepository.findById(productId);

        if (existingMV.isPresent()) {
            // 기존 MV 업데이트
            ProductMaterializedViewEntity mv = existingMV.get();
            mv.sync(product, brand, likeCount);
            mvRepository.save(mv);
            log.debug("상품 MV 갱신 완료: productId={}", productId);
        } else {
            // 신규 MV 생성
            ProductMaterializedViewEntity newMV = ProductMaterializedViewEntity.from(
                    product, brand, likeCount
            );
            mvRepository.save(newMV);
            log.debug("상품 MV 생성 완료: productId={}", productId);
        }
    }

    /**
     * 특정 브랜드의 모든 상품 MV를 동기화합니다.
     * 
     * @param brandId 브랜드 ID
     */
    @Transactional
    public void syncBrand(Long brandId) {
        log.info("브랜드 MV 동기화 시작: brandId={}", brandId);

        BrandEntity brand = brandRepository.getBrandById(brandId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("브랜드를 찾을 수 없습니다. brandId=%d", brandId)
                ));

        // 해당 브랜드의 모든 상품 조회
        Page<ProductEntity> products = productRepository.getProducts(
                new com.loopers.domain.product.dto.ProductSearchFilter(
                        brandId, null,
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
                )
        );

        if (products.isEmpty()) {
            log.info("동기화할 상품이 없습니다. brandId={}", brandId);
            return;
        }

        List<Long> productIds = products.getContent().stream()
                .map(ProductEntity::getId)
                .collect(Collectors.toList());

        // 좋아요 수 집계
        Map<Long, Long> likeCountMap = productIds.stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        likeRepository::countByProductIdAndDeletedAtIsNull
                ));

        // 기존 MV 조회
        Map<Long, ProductMaterializedViewEntity> existingMVMap = mvRepository.findByIdIn(productIds).stream()
                .collect(Collectors.toMap(ProductMaterializedViewEntity::getProductId, mv -> mv));

        // MV 생성 또는 업데이트
        List<ProductMaterializedViewEntity> toSave = new ArrayList<>();

        products.getContent().forEach(product -> {
            Long likeCount = likeCountMap.getOrDefault(product.getId(), 0L);
            ProductMaterializedViewEntity existingMV = existingMVMap.get(product.getId());
            if (existingMV == null) {
                ProductMaterializedViewEntity newMV = ProductMaterializedViewEntity.from(
                        product, brand, likeCount
                );
                toSave.add(newMV);
            } else {
                existingMV.sync(product, brand, likeCount);
                toSave.add(existingMV);
            }
        });

        if (!toSave.isEmpty()) {
            mvRepository.saveAll(toSave);
        }

        log.info("브랜드 MV 동기화 완료: brandId={}, 처리 건수={}", brandId, toSave.size());
    }
}
