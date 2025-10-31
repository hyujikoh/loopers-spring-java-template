package com.loopers.infrastructure.point;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointHistoryEntity;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {
}
