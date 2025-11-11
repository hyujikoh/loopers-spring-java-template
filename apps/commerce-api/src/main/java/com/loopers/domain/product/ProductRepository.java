package com.loopers.domain.product;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.loopers.domain.product.dto.ProductSearchFilter;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public interface ProductRepository {
    ProductEntity save(ProductEntity product);

    Page<ProductEntity> getProducts(ProductSearchFilter searchFilter);

    Optional<ProductEntity> findById(Long id);
}
