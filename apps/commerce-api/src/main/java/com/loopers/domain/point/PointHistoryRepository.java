package com.loopers.domain.point;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface PointHistoryRepository {
    PointHistoryEntity save(PointHistoryEntity pointHistory);

    /**
     * 특정 포인트의 이력을 생성일 기준 내림차순으로 조회합니다.
     *
     * @param point 포인트 엔티티
     * @return 포인트 이력 목록 (최신순)
     */
    List<PointHistoryEntity> findByPointOrderByCreatedAtDesc(PointEntity point);
}
