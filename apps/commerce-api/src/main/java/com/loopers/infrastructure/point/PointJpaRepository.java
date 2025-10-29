package com.loopers.infrastructure.point;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.user.UserEntity;

public interface PointJpaRepository extends JpaRepository<PointEntity, Long> {
    Optional<PointEntity> findByUser_Username(String username);
}
