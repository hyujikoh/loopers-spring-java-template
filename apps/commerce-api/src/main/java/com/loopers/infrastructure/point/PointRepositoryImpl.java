package com.loopers.infrastructure.point;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;


    @Override
    public PointEntity save(PointEntity point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public PointEntity findByUsername(String username) {
        return pointJpaRepository.findByUser_Username(username).orElse(null);
    }
}
