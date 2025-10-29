package com.loopers.infrastructure.point;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.user.UserEntity;

public interface PointJpaRepository extends JpaRepository<PointEntity, Long> {
    @Query("SELECT p FROM PointEntity p JOIN FETCH p.user u WHERE u.username = :username")
    Optional<PointEntity> findByUsername(@Param("username") String username);
}
