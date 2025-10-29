package com.loopers.domain.point;

import java.math.BigDecimal;

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
public class Point extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserEntity user;

    @Min(0)
    @Column(precision = 9, scale = 2, nullable = false)
    private BigDecimal amount;

    public Point(UserEntity user, BigDecimal amount) {
        this.user = user;
        this.amount = amount;
    }

    public static Point createPointEntity(UserEntity user) {
        return  new Point(user, BigDecimal.ZERO);

    }
}
