package com.loopers.application.point;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.point.PointEntity;
import com.loopers.domain.point.PointService;
import com.loopers.interfaces.api.point.PointV1Dtos;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */

@Component
@RequiredArgsConstructor
public class PointFacade {
    private final PointService pointService;

    @Transactional(readOnly = true)
    public PointV1Dtos.PointInfo getPointInfo(String username) {
        PointEntity point = pointService.getByUsername(username);

        // 포인트 정보가 없는 경우를 사용자 없는 정보로 간주.
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND_USER);
        }
        return PointV1Dtos.PointInfo.from(point);
    }

    @Transactional
    public PointV1Dtos.PointChargeResponse chargePoint(String username, PointV1Dtos.PointChargeRequest request) {
        java.math.BigDecimal totalAmount = pointService.charge(username, request.amount());

        return new PointV1Dtos.PointChargeResponse(username, totalAmount);
    }
}
