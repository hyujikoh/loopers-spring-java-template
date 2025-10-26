package com.loopers.domain.user;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
public record UserRegisterRequest(
        String username,
        String email,
        String birthdate
) {
}
