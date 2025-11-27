package com.loopers.infrastructure.cache;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캐시 전략 정의
 *
 * <p>Hot/Warm/Cold 데이터에 따른 차별화된 캐시 전략을 제공합니다.</p>
 *
 * <p>멘토링 핵심 원칙:</p>
 * <ul>
 *   <li>Hot 데이터: 배치로 미리 갱신하여 캐시 스탬피드 방지</li>
 *   <li>Warm 데이터: 일반 Cache-Aside 패턴</li>
 *   <li>Cold 데이터: 캐시하지 않음</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum CacheStrategy {

    /**
     * Hot 데이터 전략
     *
     * <p>특징:</p>
     * <ul>
     *   <li>대상: 1페이지, 인기 정렬 (가장 빈번하게 조회되는 데이터)</li>
     *   <li>TTL: 60분 (길게 설정)</li>
     *   <li>갱신 방식: 배치로 1분마다 미리 갱신 (덮어쓰기)</li>
     *   <li>목적: 캐시 스탬피드 방지 및 최고 성능 보장</li>
     * </ul>
     */
    HOT(60, TimeUnit.MINUTES, true, CacheUpdateStrategy.BATCH_REFRESH),

    /**
     * Warm 데이터 전략
     *
     * <p>특징:</p>
     * <ul>
     *   <li>대상: 2~3페이지 (꾸준히 조회되는 데이터)</li>
     *   <li>TTL: 10분 (중간 수준)</li>
     *   <li>갱신 방식: Cache-Aside 패턴 (조회 시 캐싱)</li>
     *   <li>목적: DB 부하 감소 및 적절한 최신성 유지</li>
     * </ul>
     */
    WARM(10, TimeUnit.MINUTES, false, CacheUpdateStrategy.CACHE_ASIDE),

    /**
     * Cold 데이터 전략
     *
     * <p>특징:</p>
     * <ul>
     *   <li>대상: 4페이지 이상 (거의 조회되지 않는 데이터)</li>
     *   <li>TTL: 없음 (캐시하지 않음)</li>
     *   <li>갱신 방식: 없음</li>
     *   <li>목적: 캐시 메모리 절약 (선택과 집중)</li>
     * </ul>
     */
    COLD(0, TimeUnit.MINUTES, false, CacheUpdateStrategy.NO_CACHE);

    private final long ttl;
    private final TimeUnit timeUnit;
    private final boolean useBatchUpdate;
    private final CacheUpdateStrategy updateStrategy;

    /**
     * 캐시 사용 여부 확인
     */
    public boolean shouldCache() {
        return this != COLD;
    }

    /**
     * 배치 갱신 사용 여부 확인
     */
    public boolean shouldUseBatchUpdate() {
        return useBatchUpdate;
    }
}
