package com.loopers.domain.point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(mappedBy = "point", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PointHistoryEntity> histories = new ArrayList<>();

    public PointEntity(UserEntity user) {
        Objects.requireNonNull(user, "사용자는 null 일 수 없습니다.");
        this.user = user;
        this.amount = BigDecimal.ZERO;
    }

    public static PointEntity createPointEntity(UserEntity user) {
        return new PointEntity(user);
    }

    /**
     * 포인트를 충전하고 이력을 생성합니다.
     *
     * @param chargeAmount 충전할 금액
     * @return 생성된 충전 이력
     */
    public PointHistoryEntity charge(BigDecimal chargeAmount) {
        validateChargeAmount(chargeAmount);
        validateCurrentAmount();

        BigDecimal previousAmount = this.amount;
        this.amount = this.amount.add(chargeAmount);

        PointHistoryEntity chargeHistory = PointHistoryEntity.createChargeHistory(this, chargeAmount, this.amount);
        this.histories.add(chargeHistory);

        return chargeHistory;
    }

    /**
     * 충전 금액의 유효성을 검사합니다.
     */
    private void validateChargeAmount(BigDecimal chargeAmount) {
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
    }

    /**
     * 현재 포인트 잔액의 유효성을 검사합니다.
     */
    private void validateCurrentAmount() {
        if (this.amount == null || this.amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("현재 포인트 잔액이 올바르지 않습니다.");
        }
    }

    public BigDecimal getAmount() {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
