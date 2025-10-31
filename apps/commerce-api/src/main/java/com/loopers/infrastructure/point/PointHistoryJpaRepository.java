package com.loopers.infrastructure.point;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.point.PointHistoryEntity;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {

    /**
     * 특정 사용자 포인트의 이력을 생성일 기준 내림차순으로 조회합니다.
     *
     * @param point 포인트 엔티티
     * @return 포인트 이력 목록 (최신순)
     */
    List<PointHistoryEntity> findByPointOrderByCreatedAtDesc(PointEntity point);

    /**
     * 특정 사용자 포인트의 이력을 페이징하여 조회합니다.
     *
     * @param point 포인트 엔티티
     * @param pageable 페이징 정보
     * @return 포인트 이력 페이지
     */
    Page<PointHistoryEntity> findByPoint(PointEntity point, Pageable pageable);
}
