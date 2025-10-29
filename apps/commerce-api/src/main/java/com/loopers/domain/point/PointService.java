package com.loopers.domain.point;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@Component
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public PointEntity getByUsername(String username) {
        return pointRepository.findByUsername(username);
    }

    @Transactional
    public void createPointForNewUser(UserEntity user) {
        PointEntity existingPoint = getByUsername(user.getUsername());

        if (existingPoint != null) {
            // 이미 포인트가 존재하는 경우 아무 작업도 수행하지 않음
            // TODO : 불필요한 로직인지 고민
            return;
        }

        PointEntity pointEntity = PointEntity.createPointEntity(user);
        pointRepository.save(pointEntity);
    }
}
