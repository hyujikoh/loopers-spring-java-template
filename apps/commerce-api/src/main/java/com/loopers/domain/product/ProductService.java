package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */

@Component
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        return productRepository.getProducts(searchFilter);
    }

    @Transactional(readOnly = true)
    public ProductEntity getProductDetail(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", id)
                ));
    }
}
