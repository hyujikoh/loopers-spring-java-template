package com.loopers.domain.user;

import java.time.LocalDate;

import com.loopers.domain.BaseEntity;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.Valid;

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

    @Column(unique = true)
    private String username;

    private String email;

    private LocalDate birthdate;

    public static UserEntity createUserEntity(@Valid UserRegisterRequest request) {
        if (request.username() == null || !request.username().matches("^[A-Za-z0-9]{1,10}$")) {
            throw new IllegalArgumentException("사용자명은 영문 및 숫자 10자 이내여야 합니다.");
        }

        if (request.email() == null || !request.email().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("올바른 형식의 이메일 주소여야 합니다.");
        }

        try {
            if (request.birthdate() == null) {
                throw new IllegalArgumentException("생년월일은 필수 입력값입니다.");
            }
            LocalDate.parse(request.birthdate());
        } catch (Exception e) {
            throw new IllegalArgumentException(e instanceof IllegalArgumentException ?
                    e.getMessage() : "올바른 날짜 형식이어야 합니다.");
        }

        return UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .birthdate(LocalDate.parse(request.birthdate()))
                .build();
    }


}
