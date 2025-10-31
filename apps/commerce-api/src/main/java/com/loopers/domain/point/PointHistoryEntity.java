package com.loopers.domain.point;

import java.math.BigDecimal;

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
    
    public PointHistoryEntity(PointEntity point, PointTransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter) {
        this.point = point;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }
}
