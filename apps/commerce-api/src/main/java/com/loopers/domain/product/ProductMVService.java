package com.loopers.domain.product;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.product.BatchUpdateResult;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.infrastructure.product.ProductMVQueryRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 Materialized View 서비스
 * <p>
 * MV 테이블 조회 및 배치 동기화를 담당하는 서비스입니다.
 * 상품, 브랜드, 좋아요 정보를 통합하여 조회 성능을 최적화합니다.
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
    private final ProductMVQueryRepository mvQueryRepository;

    /**
     * 상품 ID로 MV를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 상품 MV (존재하지 않으면 Optional.empty())
     */
    public ProductMaterializedViewEntity getById(Long productId) {
        return mvRepository.findById(productId).
                orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", productId)
                ));
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
     * @param brandId  브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        return mvRepository.findByBrandId(brandId, pageable);
    }

    /**
     * @param productIds 상품 ID 목록
     * @param pageable   페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByIdsAsPage(List<Long> productIds, Pageable pageable) {
        List<ProductMaterializedViewEntity> mvEntities = mvRepository.findByIdIn(productIds);
        return new PageImpl<>(mvEntities, pageable, mvEntities.size());
    }

    /**
     * 상품명으로 검색하여 MV를 페이징 조회합니다.
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> searchByName(String keyword, Pageable pageable) {
        return mvRepository.findByNameContaining(keyword, pageable);
    }

    private ZonedDateTime lastBatchTime = ZonedDateTime.now().minusYears(1); // 초기값

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

        // 변경 추적용
        Set<Long> changedProductIds = new HashSet<>();
        Set<Long> affectedBrandIds = new HashSet<>();

        try {
            log.info("MV 배치 동기화 시작 - 마지막 배치 시간: {}", lastBatchTime);

            List<ProductMVSyncDto> changedProducts = mvQueryRepository.findChangedProductsForSync(lastBatchTime);

            if (changedProducts.isEmpty()) {
                log.info("변경된 상품이 없습니다.");
                long duration = System.currentTimeMillis() - startTime;
                lastBatchTime = ZonedDateTime.now(); // 배치 시간 갱신
                return BatchUpdateResult.success(0, 0, duration, changedProductIds, affectedBrandIds);
            }

            log.info("변경 감지: {}건", changedProducts.size());

            // 2. 변경된 상품 ID 목록 추출
            List<Long> productIds = changedProducts.stream()
                    .map(ProductMVSyncDto::getProductId)
                    .collect(Collectors.toList());

            // 3. 기존 MV 조회 (변경된 상품만)
            Map<Long, ProductMaterializedViewEntity> existingMVMap = mvRepository.findByIdIn(productIds).stream()
                    .collect(Collectors.toMap(ProductMaterializedViewEntity::getProductId, mv -> mv));

            // 4. MV 생성 또는 업데이트
            List<ProductMaterializedViewEntity> toSave = new ArrayList<>();

            for (ProductMVSyncDto dto : changedProducts) {
                ProductMaterializedViewEntity existingMV = existingMVMap.get(dto.getProductId());

                if (existingMV == null) {
                    // 신규 생성
                    ProductMaterializedViewEntity newMV = ProductMaterializedViewEntity.fromDto(dto);
                    toSave.add(newMV);
                    createdCount++;

                    // 변경 추적
                    changedProductIds.add(dto.getProductId());
                    affectedBrandIds.add(dto.getBrandId());
                } else {
                    // 기존 MV와 비교하여 실제 변경이 있는 경우만 업데이트
                    boolean hasChanges = hasActualChangesFromDto(existingMV, dto);

                    if (hasChanges) {
                        syncMVFromDto(existingMV, dto);
                        toSave.add(existingMV);
                        updatedCount++;

                        // 변경 추적
                        changedProductIds.add(dto.getProductId());
                        affectedBrandIds.add(dto.getBrandId());
                    }
                }
            }

            // 5. 일괄 저장
            if (!toSave.isEmpty()) {
                mvRepository.saveAll(toSave);
            }

            // 6. 배치 시간 갱신
            lastBatchTime = ZonedDateTime.now();

            long duration = System.currentTimeMillis() - startTime;
            log.info("MV 배치 동기화 완료 - 생성: {}건, 갱신: {}건, 변경된 상품: {}개, 영향받은 브랜드: {}개, 소요: {}ms",
                    createdCount, updatedCount, changedProductIds.size(), affectedBrandIds.size(), duration);

            return BatchUpdateResult.success(
                    createdCount,
                    updatedCount,
                    duration,
                    changedProductIds,
                    affectedBrandIds
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MV 배치 동기화 실패 - 소요: {}ms", duration, e);
            return BatchUpdateResult.failure(e.getMessage(), duration);
        }
    }


    /**
     * DTO로부터 기존 MV를 동기화합니다.
     */
    private void syncMVFromDto(ProductMaterializedViewEntity mv, ProductMVSyncDto dto) {
        ProductEntity product = new ProductEntity(
                dto.getBrandId(),
                dto.getProductName(),
                dto.getProductDescription(),
                dto.getOriginPrice(),
                dto.getDiscountPrice(),
                dto.getStockQuantity()
        );

        BrandEntity brand = new BrandEntity(dto.getBrandName(), null);

        mv.sync(product, brand, dto.getLikeCount() != null ? dto.getLikeCount() : 0L);
    }

    /**
     * DTO와 기존 MV를 비교하여 실제 변경사항이 있는지 확인합니다.
     */
    private boolean hasActualChangesFromDto(ProductMaterializedViewEntity mv, ProductMVSyncDto dto) {
        // 상품명 변경
        if (!mv.getName().equals(dto.getProductName())) {
            return true;
        }

        // 가격 변경
        if (!mv.getPrice().getOriginPrice().equals(dto.getOriginPrice())) {
            return true;
        }

        if (dto.getDiscountPrice() != null && !mv.getPrice().getDiscountPrice().equals(dto.getDiscountPrice())) {
            return true;
        }

        // 재고 변경
        if (!mv.getStockQuantity().equals(dto.getStockQuantity())) {
            return true;
        }

        // 브랜드명 변경
        if (!mv.getBrandName().equals(dto.getBrandName())) {
            return true;
        }

        // 좋아요 수 변경
        Long dtoLikeCount = dto.getLikeCount() != null ? dto.getLikeCount() : 0L;
        if (!mv.getLikeCount().equals(dtoLikeCount)) {
            return true;
        }

        return false;
    }

    @Transactional
    public void deleteById(Long productId) {
        mvRepository.deleteByProductIdIn(List.of(productId));
    }
}
