package com.loopers.infrastructure.point;

import org.springframework.stereotype.Component;

import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.point.PointHistoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
@RequiredArgsConstructor
@Component
public class PointHistoryRepositoryImpl implements PointHistoryRepository {
    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistoryEntity save(PointHistoryEntity pointHistory) {
        return pointHistoryJpaRepository.save(pointHistory);
    }
}
