package com.loopers.domain.coupon;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.dto.BrandSearchFilter;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface CouponRepository {
    CouponEntity save(CouponEntity any);
}
