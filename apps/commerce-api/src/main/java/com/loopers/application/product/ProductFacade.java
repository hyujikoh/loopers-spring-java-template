package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductService productService;

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(Pageable pageable) {
        Page<ProductEntity> products = productService.getProducts(pageable);
        return products.map(ProductInfo::of);
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long id) {
        ProductEntity product = productService.getProductDetail(id);
        return ProductDetailInfo.of(product);
    }
}
