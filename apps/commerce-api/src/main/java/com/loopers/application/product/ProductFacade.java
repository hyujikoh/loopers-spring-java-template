package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductSearchFilter;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        Page<ProductEntity> products = productService.getProducts(productSearchFilter);

        // 각 상품의 브랜드 정보 조회
        return products.map(ProductInfo::of);
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long id) {
        // 1. Product 조회
        ProductEntity product = productService.getProductDetail(id);

        // 2. Brand 조회
        BrandEntity brand = brandService.getBrandById(product.getBrandId());

        // 3. DTO 조합 후 반환
        return ProductDetailInfo.of(product, brand);
    }
}
