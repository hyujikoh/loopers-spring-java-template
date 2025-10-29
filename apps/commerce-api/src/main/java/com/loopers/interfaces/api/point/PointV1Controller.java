package com.loopers.interfaces.api.point;

import org.springframework.web.bind.annotation.*;

import com.loopers.application.point.PointFacade;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@RestController
public class PointV1Controller {
    private final PointFacade pointFacade;

    @GetMapping("/api/v1/points")
    public ApiResponse<PointV1Dtos.PointInfoResponse> getPointInfo(@RequestHeader("X-USER-ID") String username) {
        PointV1Dtos.PointInfo pointInfo = pointFacade.getPointInfo(username);

        return ApiResponse.success(PointV1Dtos.PointInfoResponse.from(pointInfo));
    }
}
