package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.Objects;

import com.loopers.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id", nullable = false)
    private PointEntity point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType transactionType;

    @Column(precision = 9, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 9, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    private PointHistoryEntity(PointEntity point, PointTransactionType transactionType, BigDecimal amount,
                              BigDecimal balanceAfter) {
        validateInputs(point, transactionType, amount, balanceAfter);

        this.point = point;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    /**
     * 포인트 충전 이력을 생성합니다.
     *
     * @param point 포인트 엔티티
     * @param chargeAmount 충전 금액
     * @param balanceAfter 충전 후 잔액
     * @return 충전 이력 엔티티
     */
    public static PointHistoryEntity createChargeHistory(PointEntity point, BigDecimal chargeAmount, BigDecimal balanceAfter) {
        return new PointHistoryEntity(point, PointTransactionType.CHARGE, chargeAmount, balanceAfter);
    }

    /**
     * 포인트 사용 이력을 생성합니다.
     *
     * @param point 포인트 엔티티
     * @param useAmount 사용 금액
     * @param balanceAfter 사용 후 잔액
     * @return 사용 이력 엔티티
     */
    public static PointHistoryEntity createUseHistory(PointEntity point,BigDecimal useAmount, BigDecimal balanceAfter) {
        return new PointHistoryEntity(point, PointTransactionType.USE , useAmount, balanceAfter);
    }

    /**
     * 입력값 유효성을 검사합니다.
     */
    private void validateInputs(PointEntity point, PointTransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter) {
        if (Objects.isNull(point)) {
            throw new IllegalArgumentException("포인트 엔티티는 필수값입니다.");
        }

        if (Objects.isNull(transactionType)) {
            throw new IllegalArgumentException("거래 유형은 필수값입니다.");
        }

        if (Objects.isNull(amount) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("거래 금액은 0보다 커야 합니다.");
        }

        if (Objects.isNull(balanceAfter) || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("거래 후 잔액은 0 이상이어야 합니다.");
        }
    }
}
