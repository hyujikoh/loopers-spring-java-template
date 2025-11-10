package com.loopers.infrastructure.product;

import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    @Override
    public ProductEntity save(ProductEntity product) {
        return productJpaRepository.save(product);
    }
}
