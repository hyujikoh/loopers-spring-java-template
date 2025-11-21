package com.loopers.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

import com.loopers.interfaces.api.ApiResponse;

@Tag(name = "Product V1 API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 목록 조회",
            description = "상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<ProductV1Dtos.PageResponse<ProductV1Dtos.ProductListResponse>> getProducts(
            @Schema(name = "페이징 정보", description = "페이지 번호와 크기")
            Pageable pageable
    );

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dtos.ProductDetailResponse> getProductDetail(
            @Schema(name = "상품 ID", description = "조회할 상품의 ID")
            Long productId,

            @Schema(name = "사용자명", description = "로그인한 사용자명 (선택)")
            String username
    );
}

