package com.loopers.fixtures;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;

public class BrandTestFixture {

    public static BrandEntity createEntity(String name, String description) {
        return BrandEntity.createBrandEntity(name, description);
    }

    public static List<BrandEntity> createEntities(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEntity("브랜드" + i, "설명" + i))
                .collect(Collectors.toList());
    }

    public static List<BrandEntity> saveBrands(BrandRepository repository, int count) {
        return createEntities(count).stream()
                .map(repository::save)
                .collect(Collectors.toList());
    }

    public static BrandEntity createAndSave(BrandRepository repository, String name, String description) {
        BrandEntity brand = createEntity(name, description);
        return repository.save(brand);
    }
}
