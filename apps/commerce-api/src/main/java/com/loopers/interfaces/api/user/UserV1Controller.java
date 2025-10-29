package com.loopers.interfaces.api.user;

import org.springframework.web.bind.annotation.*;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@RestController
public class UserV1Controller {
    private final UserFacade userFacade;

    @PostMapping("/api/v1/users")
    public ApiResponse<UserV1Dtos.UserRegisterResponse> registerUser(@RequestBody @Valid UserRegisterRequest request) {
        UserInfo userInfo = userFacade.registerUser(request);

        return ApiResponse.success(UserV1Dtos.UserRegisterResponse.from(userInfo));
    }


    @GetMapping("/api/v1/users")
    public ApiResponse<UserV1Dtos.UserInfoResponse> getUserByUsername(@RequestParam String username) {
        UserInfo userInfo = userFacade.getUserByUsername(username);
        return ApiResponse.success(UserV1Dtos.UserInfoResponse.from(userInfo));
    }
}
