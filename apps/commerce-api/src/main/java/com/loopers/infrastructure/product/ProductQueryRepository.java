package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProductEntity.productEntity;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    /**
     * 상품 페이징 조회
     * @param searchFilter
     * @return
     */
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        Pageable pageable = searchFilter.pageable();

        // 공통 where 조건 빌드
        BooleanExpression whereCondition = buildWhereCondition(searchFilter);

        // 조회용 쿼리
        List<ProductEntity> content = queryFactory
                .selectFrom(productEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(productEntity.id.desc())
                .fetch();

        // 카운트 쿼리 (동일한 조건 적용)
        Long total = queryFactory
                .select(productEntity.count())
                .from(productEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 검색 필터 기반 where 조건 빌드
     * @param searchFilter
     * @return
     */
    private BooleanExpression buildWhereCondition(ProductSearchFilter searchFilter) {
        return productEntity.deletedAt.isNull()
                .and(brandIdEq(searchFilter.brandId()))
                .and(productNameContains(searchFilter.productName()));
    }

    private BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? productEntity.brandId.eq(brandId) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return productName != null ? productEntity.name.contains(productName) : null;
    }
}
