package com.loopers.domain.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.loopers.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * 쿠폰 엔티티
 *
 * <p>사용자에게 발급된 할인 쿠폰 정보를 관리합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEntity extends BaseEntity {

    /**
     * 사용자 ID (users.id 참조)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 쿠폰 타입 (FIXED_AMOUNT: 정액 할인, PERCENTAGE: 배율 할인)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 20)
    private CouponType couponType;

    /**
     * 정액 할인 금액 (정액 쿠폰용)
     */
    @Column(name = "fixed_amount", precision = 10, scale = 2)
    private BigDecimal fixedAmount;

    /**
     * 할인 비율 (배율 쿠폰용, 0-100)
     */
    @Column(name = "percentage", precision = 5, scale = 2)
    private Integer percentage;

    /**
     * 쿠폰 상태 (UNUSED: 미사용, USED: 사용됨)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status = CouponStatus.UNUSED;

    /**
     * 정액 쿠폰 생성자
     */
    public CouponEntity(Long userId, BigDecimal fixedAmount) {
        validateUserId(userId);
        validateFixedAmount(fixedAmount);

        this.userId = userId;
        this.couponType = CouponType.FIXED_AMOUNT;
        this.fixedAmount = fixedAmount;
        this.percentage = null;
        this.status = CouponStatus.UNUSED;
    }

    /**
     * 배율 쿠폰 생성자
     */
    public CouponEntity(Long userId, Integer percentage) {
        validateUserId(userId);
        validatePercentage(percentage);

        this.userId = userId;
        this.couponType = CouponType.PERCENTAGE;
        this.fixedAmount = null;
        this.percentage = percentage;
        this.status = CouponStatus.UNUSED;
    }

    /**
     * 정액 쿠폰 생성
     */
    public static CouponEntity createFixedAmountCoupon(Long userId, BigDecimal fixedAmount) {
        return new CouponEntity(userId, fixedAmount);
    }

    /**
     * 배율 쿠폰 생성
     */
    public static CouponEntity createPercentageCoupon(Long userId, Integer percentage) {
        return new CouponEntity(userId, percentage);
    }

    /**
     * 쿠폰 사용 처리
     *
     * @throws IllegalStateException 이미 사용된 쿠폰인 경우
     */
    public void use() {
        if (this.status == CouponStatus.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
    }

    /**
     * 쿠폰 사용 여부 확인
     */
    public boolean isUsed() {
        return this.status == CouponStatus.USED;
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public boolean canUse() {
        return this.status == CouponStatus.UNUSED;
    }

    /**
     * 할인 금액 계산
     *
     * @param productPrice 상품 가격
     * @return 할인 금액
     */
    public BigDecimal calculateDiscount(BigDecimal productPrice) {
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }

        return switch (this.couponType) {
            case FIXED_AMOUNT -> this.fixedAmount;
            case PERCENTAGE -> productPrice.multiply(BigDecimal.valueOf(this.percentage))
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        };
    }

    /**
     * 쿠폰 소유권 검증
     *
     * @param userId 검증할 사용자 ID
     * @throws IllegalArgumentException 소유자가 아닌 경우
     */
    public void validateOwnership(Long userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw new IllegalArgumentException("쿠폰 소유자만 사용할 수 있습니다.");
        }
    }

    // 검증 메서드들
    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    private static void validateFixedAmount(BigDecimal fixedAmount) {
        if (fixedAmount == null || fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정액 할인 금액은 0보다 커야 합니다.");
        }
    }

    private static void validatePercentage(Integer percentage) {
        if (percentage == null || percentage <= 0 || percentage > 100) {
            throw new IllegalArgumentException("할인 비율은 0보다 크고 100 이하여야 합니다.");
        }
    }

    @Override
    protected void guard() {
        // 쿠폰 타입별 필수 필드 검증
        switch (this.couponType) {
            case FIXED_AMOUNT -> {
                if (this.fixedAmount == null || this.fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("정액 쿠폰은 할인 금액이 필수입니다.");
                }
                if (this.percentage != null) {
                    throw new IllegalStateException("정액 쿠폰은 할인 비율을 가질 수 없습니다.");
                }
            }
            case PERCENTAGE -> {
                if (this.percentage == null || this.percentage <= 0 || this.percentage > 100) {
                    throw new IllegalStateException("배율 쿠폰은 0-100 범위의 할인 비율이 필수입니다.");
                }
                if (this.fixedAmount != null) {
                    throw new IllegalStateException("배율 쿠폰은 정액 할인 금액을 가질 수 없습니다.");
                }
            }
        }
    }
}
