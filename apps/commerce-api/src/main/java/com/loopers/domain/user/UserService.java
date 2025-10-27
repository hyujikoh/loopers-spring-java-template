package com.loopers.domain.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@Component
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserEntity register(UserRegisterRequest userRegisterRequest) {

        UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

        return userRepository.save(userEntity);
    }
}
