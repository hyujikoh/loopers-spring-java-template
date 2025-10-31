package com.loopers.domain.point;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
public interface PointRepository {
    PointEntity save(PointEntity user);

    PointEntity findByUsername(String username);
}
