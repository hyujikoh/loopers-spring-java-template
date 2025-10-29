package com.loopers.domain.point;

import org.springframework.stereotype.Component;

import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@Component
@RequiredArgsConstructor
public class PointService {
    public Point getByUser(UserEntity user) {
        return null;
    }
}
