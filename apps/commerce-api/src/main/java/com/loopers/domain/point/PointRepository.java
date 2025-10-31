package com.loopers.domain.point;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
public interface PointRepository {
    PointEntity save(PointEntity user);

    Optional<PointEntity> findByUsername(String username);
}
