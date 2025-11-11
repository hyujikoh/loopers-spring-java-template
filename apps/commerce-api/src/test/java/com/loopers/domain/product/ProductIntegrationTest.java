package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@SpringBootTest
@DisplayName("Product 통합 테스트")
public class ProductIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductFacade productFacade;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("상품 저장 및 조회")
    class ListProductsTest {

        @Test
        @DisplayName("새로운 상품 등록 시 상품이 저장된다")
        void save_product_with_brand() {
            // given
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Nike", "Just do it");

            // when
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Air Max",
                    "편안한 운동화",
                    new BigDecimal("120000"),
                    50
            );

            // then
            assertThat(product.getId()).isNotNull();
            assertThat(product.getName()).isEqualTo("Air Max");
            assertThat(product.getBrandId()).isEqualTo(brand.getId());
        }

        @Test
        @DisplayName("상품 목록을 페이징하여 조회할 수 있다")
        void get_product_pagination() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            Pageable pageable = PageRequest.of(0, 5);
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(null, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(5);
            assertThat(productInfos.getTotalElements()).isEqualTo(10);

            ProductInfo firstProduct = productInfos.getContent().get(0);
            assertThat(firstProduct.name()).isNotNull();
            assertThat(firstProduct.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(firstProduct.likesCount()).isNotNull();
        }

        @Test
        @DisplayName("상품 상세를 조회할 수 있다")
        void get_product_detail_success() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            // when
            ProductDetailInfo productDetail = productFacade.getProductDetail(1L);

            // then
            assertThat(productDetail).isNotNull();
            assertThat(productDetail.name()).isNotNull();
            assertThat(productDetail.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(productDetail.brand()).isNotNull();
        }
    }

    @Test
    @DisplayName("브랜드 ID로 상품을 필터링하여 조회할 수 있다")
    void filter_products_by_brand() {
        // given
        ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

        Pageable pageable = PageRequest.of(0, 25);
        ProductSearchFilter productSearchFilter = new ProductSearchFilter(1L, null, pageable);

        // when
        Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

        // then
        assertThat(productInfos).isNotNull();
        assertThat(productInfos.getContent()).hasSize(5);
        assertThat(productInfos.getTotalElements()).isEqualTo(5);

        productInfos.getContent().forEach(productInfo -> {
            assertThat(productInfo.brandId()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("상품이 없는 경우 빈 목록을 반환한다")
    void return_empty_list_when_no_products() {
        Pageable pageable = PageRequest.of(0, 25);
        ProductSearchFilter productSearchFilter = new ProductSearchFilter(1L, null, pageable);

        // when
        Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

        // then
        assertThat(productInfos).isNotNull();
        assertThat(productInfos.getContent()).hasSize(0);
        assertThat(productInfos.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 ProductNotFoundException이 발생한다")
    void throw_exception_when_product_not_found() {
        // given
        Long nonExistentId = 999L;

        // when & then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                com.loopers.support.error.CoreException.class,
                () -> productFacade.getProductDetail(nonExistentId)
        ).getErrorType()).isEqualTo(com.loopers.support.error.ErrorType.NOT_FOUND_PRODUCT);
    }

    @Test
    @DisplayName("최신순으로 상품을 정렬하여 조회할 수 있다")
    void get_products_sorted_by_latest() {
        // given
        ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5); // 1개 브랜드, 5개 상품 생성

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "생성일시"));
        ProductSearchFilter productSearchFilter = new ProductSearchFilter(null, null, pageable);

        // when
        Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

        // then
        assertThat(productInfos).isNotNull();
        assertThat(productInfos.getContent()).hasSize(5);
        assertThat(productInfos.getTotalElements()).isEqualTo(5);

        // 최신순 정렬 검증 (생성일시 내림차순)
        List<java.time.ZonedDateTime> createdAts = productInfos.getContent().stream()
                .map(ProductInfo::createdAt)  // ProductInfo에 생성일시 필드가 있다고 가정
                .toList();
        assertThat(createdAts).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("존재하지 않는 브랜드로 상품을 필터링하면 빈 목록을 반환한다")
    void return_empty_list_when_filtering_by_non_existent_brand() {
        // given
        ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

        Pageable pageable = PageRequest.of(0, 25);
        ProductSearchFilter productSearchFilter = new ProductSearchFilter(9L, null, pageable);

        // when
        Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

        // then
        assertThat(productInfos).isNotNull();
        assertThat(productInfos.getContent()).hasSize(0);
        assertThat(productInfos.getTotalElements()).isEqualTo(0);
    }
}
