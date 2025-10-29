package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRegisterRequest;

public record UserRegisterCommand(
        String username,
        String email,
        String birthdate,
        Gender gender
) {
    public static UserRegisterCommand of(String username, String email, String birthdate, Gender gender) {
        return new UserRegisterCommand(username, email, birthdate, gender);
    }

    public UserRegisterRequest toDomainRequest() {
        return UserRegisterRequest.of(username, email, birthdate, gender);
    }
}
