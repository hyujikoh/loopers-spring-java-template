package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 상품 엔티티
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "brand_id",
        nullable = false
    )
    private BrandEntity brand;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Price price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    /**
     * 상품을 생성한다.
     *
     * @param request 상품 생성 요청 정보
     * @return 생성된 상품 엔티티
     */
    public static ProductEntity createProduct(ProductDomainRequest request) {
        ProductEntity product = new ProductEntity();
        product.brand = request.brand();
        product.name = request.name();
        product.description = request.description();
        product.price = Price.of(request.originPrice(), request.discountPrice());
        product.stockQuantity = request.stockQuantity();
        product.likeCount = 0L;

        return product;
    }

    /**
     * 상품 정보를 수정한다.
     * TODO : 향후 상품 상태(판매중지 등) 관련 필드가 추가되면 수정 필요
     *
     * @param name 상품명
     * @param description 상품 설명
     */
    public void updateBasicInfo(String name, String description) {
        validateName(name);
        this.name = name;
        this.description = description;
    }

    /**
     * 가격 정보를 수정한다.
     *
     * @param originPrice 정가
     * @param discountPrice 할인가
     */
    public void updatePrice(BigDecimal originPrice, BigDecimal discountPrice) {
        this.price = Price.of(originPrice, discountPrice);
    }

    /**
     * 할인가를 적용한다.
     *
     * @param discountPrice 할인가
     */
    public void applyDiscount(BigDecimal discountPrice) {
        this.price.applyDiscount(discountPrice);
    }

    /**
     * 할인을 제거한다.
     */
    public void removeDiscount() {
        this.price.removeDiscount();
    }

    /**
     * 재고를 추가한다.
     *
     * @param quantity 추가할 재고 수량
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("추가할 재고 수량은 0보다 커야 합니다.");
        }
        this.stockQuantity += quantity;
    }

    /**
     * 재고를 차감한다.
     *
     * @param quantity 차감할 재고 수량
     */
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 재고 수량은 0보다 커야 합니다.");
        }

        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("재고가 부족합니다. (요청: " + quantity + ", 보유: " + this.stockQuantity + ")");
        }

        this.stockQuantity -= quantity;
    }

    /**
     * 재고 수량을 설정한다.
     *
     * @param stockQuantity 재고 수량
     */
    public void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 음수일 수 없습니다.");
        }
        this.stockQuantity = stockQuantity;
    }

    /**
     * 좋아요 수를 증가시킨다.
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수를 감소시킨다.
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 재고 여부를 확인한다.
     *
     * @return 재고가 있는지 여부
     */
    public boolean hasStock() {
        return this.stockQuantity > 0;
    }

    /**
     * 지정된 수량만큼 주문 가능한지 확인한다.
     * TODO : 향후 예약 주문 기능이 추가되면 수정 필요
     * @param quantity 주문 수량
     * @return 주문 가능 여부
     */
    public boolean canOrder(int quantity) {
        return this.stockQuantity >= quantity && quantity > 0;
    }

    /**
     * 실제 판매 가격을 반환한다.
     *
     * @return 실제 판매 가격
     */
    public BigDecimal getSellingPrice() {
        return this.price.getSellingPrice();
    }

    /**
     * 할인 여부를 확인한다.
     *
     * @return 할인 상품인지 여부
     */
    public boolean isDiscounted() {
        return this.price.isDiscounted();
    }

    @Override
    protected void guard() {
        validateName(this.name);
        validateStockQuantity(this.stockQuantity);
        validateLikeCount(this.likeCount);

        if (this.brand == null) {
            throw new IllegalStateException("브랜드는 필수입니다.");
        }

        if (this.price == null) {
            throw new IllegalStateException("가격 정보는 필수입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException("상품명은 200자를 초과할 수 없습니다.");
        }
    }

    private void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
    }

    private void validateLikeCount(Long likeCount) {
        if (likeCount == null || likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }
    }
}
