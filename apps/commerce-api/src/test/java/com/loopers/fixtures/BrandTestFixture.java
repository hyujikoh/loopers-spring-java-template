package com.loopers.fixtures;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public class BrandTestFixture {

    public static BrandEntity createEntity(String name, String description) {
        return BrandEntity.createBrandEntity(name, description);
    }

    public static List<BrandEntity> createEntities(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEntity("브랜드" + i, "설명" + i))
                .collect(Collectors.toList());
    }

    public static List<BrandEntity> saveBrands(BrandRepository brandRepository, int count) {
        List<BrandEntity> brands = createEntities(count);
        brands.forEach(brandRepository::save);
        return brands;
    }
}
