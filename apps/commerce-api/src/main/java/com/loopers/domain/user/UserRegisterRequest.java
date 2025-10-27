package com.loopers.domain.user;

import jakarta.validation.constraints.NotNull;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
public record UserRegisterRequest(
        @NotNull
        String username,

        @NotNull
        String email,

        @NotNull
        String birthdate,

        @NotNull
        Gender gender
) {
}
