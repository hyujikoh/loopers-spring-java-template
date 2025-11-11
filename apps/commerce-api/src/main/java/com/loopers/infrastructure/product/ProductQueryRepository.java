package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProductEntity.productEntity;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductEntity;
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
     * @param pageable
     * @return
     */
    public Page<ProductEntity> getProducts(Pageable pageable) {
        // 조회용 쿼리
        List<ProductEntity> content = queryFactory
                .selectFrom(productEntity)
                .where(productEntity.deletedAt.isNull())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(productEntity.id.desc())
                .fetch();

        // 카운트 쿼리
        Long total = queryFactory
                .select(productEntity.count())
                .from(productEntity)
                .where(productEntity.deletedAt.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
