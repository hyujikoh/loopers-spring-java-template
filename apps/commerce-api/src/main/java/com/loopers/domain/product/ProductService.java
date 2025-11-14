package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * 상품 도메인 서비스
 *
 * 상품 도메인의 비즈니스 로직을 처리합니다.
 * 단일 책임 원칙에 따라 상품 Repository에만 의존합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    /**
     * 검색 필터 조건으로 상품 목록을 조회합니다.
     *
     * @param searchFilter 검색 필터 (브랜드ID, 정렬 조건 등)
     * @return 검색된 상품 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        return productRepository.getProducts(searchFilter);
    }

    /**
     * 상품 ID로 상품 상세 정보를 조회합니다.
     *
     * @param id 상품 ID
     * @return 조회된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ProductEntity getProductDetail(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    /**
     * 상품을 등록합니다.
     *
     * 상품 등록은 단일 도메인 작업이므로 도메인 서비스에서 트랜잭션 처리합니다.
     *
     * @param request 상품 생성 요청 정보
     * @return 등록된 상품 엔티티
     */
    @Transactional
    public ProductEntity registerProduct(@Valid ProductDomainCreateRequest request) {
        // 상품 엔티티 생성
        ProductEntity productEntity = ProductEntity.createEntity(request);

        return productRepository.save(productEntity);
    }

    /**
     * 상품 재고를 차감합니다.
     *
     * 재고 차감은 상품 도메인의 핵심 비즈니스 로직입니다.
     *
     * @param productId 상품 ID
     * @param quantity 차감할 재고 수량
     * @return 재고가 차감된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없거나 재고가 부족한 경우
     */
    @Transactional
    public ProductEntity deductStock(Long productId, int quantity) {
        ProductEntity product = getProductDetail(productId);
        product.deductStock(quantity);
        return productRepository.save(product);
    }

    /**
     * 상품의 좋아요 수를 증가시킵니다.
     *
     * @param productId 상품 ID
     * @return 좋아요 수가 증가된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional
    public ProductEntity increaseLikeCount(Long productId) {
        ProductEntity product = getProductDetail(productId);
        product.increaseLikeCount();
        return productRepository.save(product);
    }

    /**
     * 상품의 좋아요 수를 감소시킵니다.
     *
     * @param productId 상품 ID
     * @return 좋아요 수가 감소된 상품 엔티티
     * @throws CoreException 상품을 찾을 수 없는 경우
     */
    @Transactional
    public ProductEntity decreaseLikeCount(Long productId) {
        ProductEntity product = getProductDetail(productId);
        product.decreaseLikeCount();
        return productRepository.save(product);
    }
}
