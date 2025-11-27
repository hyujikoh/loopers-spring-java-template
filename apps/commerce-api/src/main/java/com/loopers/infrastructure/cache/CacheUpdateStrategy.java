package com.loopers.infrastructure.cache;

/**
 * 캐시 갱신 전략
 *
 * <p>캐시 데이터를 최신 상태로 유지하는 방법을 정의합니다.</p>
 */
public enum CacheUpdateStrategy {

    /**
     * 배치 갱신 전략
     *
     * <p>주기적인 배치 작업으로 캐시를 미리 갱신합니다.</p>
     *
     * <p>장점:</p>
     * <ul>
     *   <li>캐시 스탬피드 방지 (캐시가 비는 순간 없음)</li>
     *   <li>안정적인 응답 시간 보장</li>
     *   <li>DB 부하 예측 가능</li>
     * </ul>
     *
     * <p>단점:</p>
     * <ul>
     *   <li>배치 작업 구현 필요</li>
     *   <li>갱신 주기만큼 데이터 지연 발생</li>
     * </ul>
     *
     * <p>적용 대상: Hot 데이터</p>
     */
    BATCH_REFRESH,

    /**
     * Cache-Aside 패턴
     *
     * <p>조회 시점에 캐시 미스가 발생하면 DB에서 조회 후 캐싱합니다.</p>
     *
     * <p>장점:</p>
     * <ul>
     *   <li>구현이 간단함</li>
     *   <li>실제 조회되는 데이터만 캐싱 (메모리 효율적)</li>
     * </ul>
     *
     * <p>단점:</p>
     * <ul>
     *   <li>캐시 미스 시 응답 시간 증가</li>
     *   <li>TTL 만료 시 캐시 스탬피드 위험</li>
     * </ul>
     *
     * <p>적용 대상: Warm 데이터</p>
     */
    CACHE_ASIDE,

    /**
     * 캐시 미사용
     *
     * <p>캐시를 사용하지 않고 항상 DB에서 직접 조회합니다.</p>
     *
     * <p>적용 대상: Cold 데이터</p>
     */
    NO_CACHE
}
