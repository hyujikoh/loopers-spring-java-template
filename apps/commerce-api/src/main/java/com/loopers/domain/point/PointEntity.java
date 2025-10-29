package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */

@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointEntity extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserEntity user;

    @Min(0)
    @Column(precision = 9, scale = 2, nullable = false)
    private BigDecimal amount;

    public PointEntity(UserEntity user) {
        Objects.requireNonNull(user, "사용자는 null 일 수 없습니다.");
        this.user = user;
        this.amount = BigDecimal.ZERO;
    }

    public static PointEntity createPointEntity(UserEntity user) {
        return  new PointEntity(user);

    }
}
