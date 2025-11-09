package com.loopers.infrastructure.brand;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.user.UserEntity;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
}
