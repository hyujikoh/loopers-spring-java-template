package com.loopers.infrastructure.brand;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public BrandEntity save(BrandEntity brand) {
        return brandJpaRepository.save(brand);
    }

    @Override
    public Page<BrandEntity> listBrands(Pageable pageable) {
        return brandJpaRepository.findAllByDeletedAtNull(pageable);
    }

    @Override
    public Optional<BrandEntity> getBrandById(long id) {
        return brandJpaRepository.findByIdAndDeletedAtNull(id);
    }
}
