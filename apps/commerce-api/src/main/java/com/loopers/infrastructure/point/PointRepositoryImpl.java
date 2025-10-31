package com.loopers.infrastructure.point;

import java.util.Optional;

import javax.swing.text.html.Option;

import org.springframework.stereotype.Component;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.point.PointRepository;

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
    public Optional<PointEntity> findByUsername(String username) {
        return pointJpaRepository.findByUsername(username);
    }
}
