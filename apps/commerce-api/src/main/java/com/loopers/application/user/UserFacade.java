package com.loopers.application.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.point.PointRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;
    private final PointService pointService;

    public UserInfo registerUser(UserRegisterRequest request) {
        // 회원 가입
        UserEntity register = userService.register(request);

        // 신규 회원 포인트 생성
        pointService.createPointForNewUser(register);
        return UserInfo.from(register);
    }

    public UserInfo getUserByUsername(@NotNull String username) {
        return Optional.ofNullable(userService.getUserByUsername(username))
                .map(UserInfo::from)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "user not found for username: " + username));
    }
}
