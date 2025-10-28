package com.loopers.application.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

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

    public UserInfo registerUser(UserRegisterRequest request) {
        UserEntity register = userService.register(request);
        return UserInfo.from(register);
    }

    public UserInfo getUserByUsername(@NotNull String username) {
        return Optional.ofNullable(userService.getUserByUsername(username))
                .map(UserInfo::from)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "user not found for username: " + username));
    }
}
