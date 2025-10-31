package com.loopers.infrastructure.point;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.point.PointHistoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistoryEntity save(PointHistoryEntity pointHistory) {
        return pointHistoryJpaRepository.save(pointHistory);
    }

    @Override
    public List<PointHistoryEntity> findByPointOrderByCreatedAtDesc(PointEntity point) {
        return pointHistoryJpaRepository.findByPointOrderByCreatedAtDesc(point);
    }

    @Override
    public Page<PointHistoryEntity> findByPoint(PointEntity point, Pageable pageable) {
        return pointHistoryJpaRepository.findByPoint(point, pageable);
    }
}
