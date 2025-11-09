package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface BrandRepository {
    BrandEntity save(BrandEntity brand);

    Page<BrandEntity> listBrands(Pageable pageable);
}
