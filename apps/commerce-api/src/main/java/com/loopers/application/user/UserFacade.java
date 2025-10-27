package com.loopers.application.user;

import org.springframework.stereotype.Component;

import com.loopers.application.example.ExampleInfo;
import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRegisterRequest;
import com.loopers.domain.user.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo registerUser(UserRegisterRequest request) {
        UserEntity register = userService.register(request);
        return UserInfo.from(register);
    }
}
