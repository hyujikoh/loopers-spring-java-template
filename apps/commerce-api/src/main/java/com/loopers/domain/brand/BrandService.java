package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
@RequiredArgsConstructor
@Component
public class BrandService {
    private final BrandRepository brandRepository;


    public Page<BrandEntity> listBrands(Pageable pageable) {
        return brandRepository.listBrands(pageable);
    }
}
