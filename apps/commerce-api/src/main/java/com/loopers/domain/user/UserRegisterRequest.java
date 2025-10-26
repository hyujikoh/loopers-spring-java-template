package com.loopers.domain.user;

import java.time.LocalDate;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
public record UserRegisterRequest(
    String username,
    String email,
    LocalDate birthdate
) {
}
