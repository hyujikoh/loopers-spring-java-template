package com.loopers.infrastructure.product;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.product.BatchUpdateResult;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductMVService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductMaterializedView 배치 스케줄러
 * 
 * <p>2분 간격으로 MV 테이블을 동기화하고, 20분마다 Hot 캐시를 갱신합니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMVBatchScheduler {
    private final ProductMVService mvService;
    private final ProductCacheService cacheService;
    
    /**
     * MV 테이블 동기화 배치 작업
     * 
     * <p>2분(120초) 간격으로 실행되며, 변경된 데이터를 MV 테이블에 반영합니다.</p>
     * <p>변경된 상품과 영향받은 브랜드의 캐시만 선택적으로 무효화합니다.</p>
     */
    @Scheduled(fixedDelay = 120000) // 2분 간격
    public void syncMaterializedView() {
        log.info("=== MV 배치 업데이트 시작 ===");

        try {
            BatchUpdateResult result = mvService.syncMaterializedView();
            
            if (!result.isSuccess()) {
                log.error("MV 배치 업데이트 실패 - error: {}", result.getErrorMessage());
                return;
            }

            // 변경사항이 있는 경우 Incremental 캐시 무효화
            if (result.hasChanges()) {
                log.info("변경사항 감지 - 선택적 캐시 무효화 시작");
                log.info("- 변경된 상품: {}개", result.getChangedProductIds().size());
                log.info("- 영향받은 브랜드: {}개", result.getAffectedBrandIds().size());

                // 변경된 상품과 브랜드의 캐시만 무효화
                cacheService.evictCachesAfterMVSync(
                    result.getChangedProductIds(),
                    result.getAffectedBrandIds()
                );
            } else {
                log.info("변경사항 없음 - 캐시 무효화 건너뜀");
            }
            
            log.info("=== MV 배치 업데이트 완료 === 생성: {}건, 갱신: {}건, 소요: {}ms",
                    result.getCreatedCount(), result.getUpdatedCount(), result.getDurationMs());

        } catch (Exception e) {
            log.error("MV 배치 업데이트 중 예상하지 못한 오류 발생", e);
        }
    }
    
    /**
     * Hot 캐시 갱신 배치 작업
     * 
     * <p>20분마다 실행되며, 캐시 스탬피드를 방지하기 위해 TTL 만료 전에 캐시를 갱신합니다.</p>
     */
    @Scheduled(cron = "0 */20 * * * *") // 20분마다
    public void refreshHotCache() {
        log.info("Hot 캐시 갱신 시작");
        
        try {
            // TODO: Hot 데이터 캐시 갱신 로직 구현
            // - 전체 상품 목록 첫 2페이지 캐시 갱신
            // - 각 브랜드별 상품 목록 첫 2페이지 캐시 갱신
            
            log.info("Hot 캐시 갱신 완료");
        } catch (Exception e) {
            log.error("Hot 캐시 갱신 실패", e);
        }
    }
}
