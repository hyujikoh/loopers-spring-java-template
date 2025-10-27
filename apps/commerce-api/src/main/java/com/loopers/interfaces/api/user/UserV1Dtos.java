package com.loopers.interfaces.api.user;

import java.time.LocalDate;

import com.loopers.application.user.UserInfo;

public class UserV1Dtos {
    public record UserRegisterResponse(Long id,
                                       String username,
                                       String email,
                                       LocalDate birthdate) {
        public static UserRegisterResponse from(UserInfo user) {
            return new UserRegisterResponse(
                    user.id(),
                    user.username(),
                    user.email(),
                    user.birthdate()
            );
        }
    }
}
