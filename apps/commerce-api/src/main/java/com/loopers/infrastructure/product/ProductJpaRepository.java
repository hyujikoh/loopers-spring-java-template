package com.loopers.infrastructure.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.user.UserEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

}
