package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.loopers.domain.product.QProductMaterializedViewEntity.productMaterializedViewEntity;

/**
 * 상품 Materialized View QueryDSL 리포지토리
 * 
 * <p>QueryDSL을 활용한 동적 쿼리 및 복잡한 조회 로직을 처리합니다.
 * 페이징, 정렬, 필터링 등의 기능을 제공합니다.</p>
 * 
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Component
@RequiredArgsConstructor
public class ProductMVQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 전체 상품 MV를 페이징 조회합니다.
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findAll(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 브랜드별 상품 MV를 페이징 조회합니다.
     * 
     * @param brandId 브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        BooleanExpression whereCondition = brandIdEq(brandId);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 상품명으로 검색하여 MV를 페이징 조회합니다.
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByNameContaining(String keyword, Pageable pageable) {
        BooleanExpression whereCondition = nameContains(keyword);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * Pageable 기반 정렬 조건을 빌드합니다.
     * 
     * @param pageable 페이징 정보
     * @return 정렬 조건 목록
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            // 기본 정렬: 좋아요 수 내림차순
            return List.of(productMaterializedViewEntity.likeCount.desc());
        }

        return pageable.getSort().stream()
                .map(sort -> {
                    String property = sort.getProperty();
                    boolean isAsc = sort.isAscending();
                    return switch (property) {
                        case "likeCount" -> isAsc ? 
                                productMaterializedViewEntity.likeCount.asc() : 
                                productMaterializedViewEntity.likeCount.desc();
                        case "name" -> isAsc ? 
                                productMaterializedViewEntity.name.asc() : 
                                productMaterializedViewEntity.name.desc();
                        case "price" -> isAsc ? 
                                productMaterializedViewEntity.price.originPrice.asc() : 
                                productMaterializedViewEntity.price.originPrice.desc();
                        case "createdAt" -> isAsc ? 
                                productMaterializedViewEntity.createdAt.asc() : 
                                productMaterializedViewEntity.createdAt.desc();
                        case "updatedAt" -> isAsc ? 
                                productMaterializedViewEntity.updatedAt.asc() : 
                                productMaterializedViewEntity.updatedAt.desc();
                        default -> productMaterializedViewEntity.likeCount.desc();
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * 브랜드 ID 동등 조건을 생성합니다.
     * 
     * @param brandId 브랜드 ID
     * @return 조건식
     */
    private BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? productMaterializedViewEntity.brandId.eq(brandId) : null;
    }

    /**
     * 상품명 포함 조건을 생성합니다.
     * 
     * @param keyword 검색 키워드
     * @return 조건식
     */
    private BooleanExpression nameContains(String keyword) {
        return keyword != null && !keyword.isBlank() ? 
                productMaterializedViewEntity.name.contains(keyword) : null;
    }
}
