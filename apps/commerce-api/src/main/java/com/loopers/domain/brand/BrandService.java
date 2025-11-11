package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.dto.BrandSearchFilter;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
@RequiredArgsConstructor
@Component
public class BrandService {
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public Page<BrandEntity> listBrands(Pageable pageable) {
        return brandRepository.listBrands(pageable);
    }

    @Transactional(readOnly = true)
    public BrandEntity getBrandById(long id) {
        return brandRepository.getBrandById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public BrandEntity getBrandByName(String name) {
        return brandRepository.findByName(name)
                .orElseThrow(() -> new BrandNotFoundException(name));
    }

    @Transactional(readOnly = true)
    public Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable) {
        return brandRepository.searchBrands(filter, pageable);
    }
}
