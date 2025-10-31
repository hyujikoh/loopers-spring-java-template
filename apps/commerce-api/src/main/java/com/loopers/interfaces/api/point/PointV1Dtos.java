package com.loopers.interfaces.api.point;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.point.PointEntity;
import com.loopers.domain.user.Gender;

public class PointV1Dtos {

    public record PointInfoResponse(
            String username,
            BigDecimal currentPointAmount
    ) {
        public static PointInfoResponse from(PointInfo pointInfo) {
            return new PointInfoResponse(
                    pointInfo.username(),
                    pointInfo.currentPointAmount()
            );
        }
    }

    public record PointInfo(
            String username,
            BigDecimal currentPointAmount
    ){
        public static PointInfo from(PointEntity pointEntity) {
            return new PointInfo(
                    pointEntity.getUser().getUsername(),
                    pointEntity.getAmount()
            );
        }
    }

    public record PointChargeRequest(
            BigDecimal amount
    ) {}

    public record PointChargeResponse(
            String username,
            BigDecimal totalAmount
    ) {}
}
