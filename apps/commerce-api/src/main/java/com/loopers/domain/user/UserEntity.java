package com.loopers.domain.user;

import java.time.LocalDate;
import java.util.Objects;

import com.loopers.domain.BaseEntity;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.Valid;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

    @Column(unique = true)
    private String username;

    private String email;

    private LocalDate birthdate;

    @Enumerated
    @Column(length = 10, nullable = false)
    private Gender gender;

    public static UserEntity createUserEntity(@Valid UserDomainCreateRequest request) {
        if (Objects.isNull(request.username()) || !request.username().matches("^[A-Za-z0-9]{1,10}$")) {
            throw new IllegalArgumentException("사용자명은 영문 및 숫자 10자 이내여야 합니다.");
        }

        if (Objects.isNull(request.email()) || !request.email().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("올바른 형식의 이메일 주소여야 합니다.");
        }

        if(Objects.isNull(request.gender())) {
            throw new IllegalArgumentException("성별은 필수 입력값입니다.");
        }

        try {
            if (Objects.isNull(request.birthdate())) {
                throw new IllegalArgumentException("생년월일은 필수 입력값입니다.");
            }
            LocalDate.parse(request.birthdate());
        } catch (Exception e) {
            throw new IllegalArgumentException(e instanceof IllegalArgumentException ?
                    e.getMessage() : "올바른 날짜 형식이어야 합니다.");
        }

        return new UserEntity(
                request.username(),
                request.email(),
                LocalDate.parse(request.birthdate()),
                request.gender());
    }

    private UserEntity(String username, String email, LocalDate birthdate, Gender gender) {
        if (Objects.isNull(username) || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수값입니다.");
        }
        if (Objects.isNull(email) || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수값입니다.");
        }
        if (Objects.isNull(birthdate)) {
            throw new IllegalArgumentException("생년월일은 필수값입니다.");
        }
        if (Objects.isNull(gender)) {
            throw new IllegalArgumentException("성별은 필수값입니다.");
        }
        if (birthdate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 현재 날짜보다 이후일 수 없습니다.");
        }

        this.username = username;
        this.email = email;
        this.birthdate = birthdate;
        this.gender = gender;
    }


}
