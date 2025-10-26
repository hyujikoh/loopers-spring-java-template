package com.loopers.domain.user;

import java.time.LocalDate;

import com.loopers.domain.BaseEntity;

import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
@Entity
@Table(name = "users")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntity extends BaseEntity {
    private String username;

    private String email;

    private LocalDate birthdate;

    public static UserEntity createUserEntity(UserRegisterRequest request) {
        return UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .birthdate(LocalDate.parse(request.birthdate()))
                .build();
    }
}
