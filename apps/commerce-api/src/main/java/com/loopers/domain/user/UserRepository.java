package com.loopers.domain.user;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
public interface UserRepository {
    UserEntity save(UserEntity userEntity);
}
