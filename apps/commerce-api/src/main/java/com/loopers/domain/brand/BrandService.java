package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.dto.BrandSearchFilter;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_BRAND,
                        String.format("브랜드를 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    @Transactional(readOnly = true)
    public BrandEntity getBrandByName(String name) {
        return brandRepository.findByName(name)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_BRAND,
                        String.format("브랜드를 찾을 수 없습니다. (이름: %s)", name)
                ));
    }

    @Transactional(readOnly = true)
    public Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable) {
        return brandRepository.searchBrands(filter, pageable);
    }
}
