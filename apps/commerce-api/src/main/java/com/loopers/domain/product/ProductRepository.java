package com.loopers.domain.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public interface ProductRepository {
    ProductEntity save(ProductEntity product);

    Page<ProductEntity> getProducts(Pageable pageable);

    Optional<ProductEntity> findById(Long id);
}
