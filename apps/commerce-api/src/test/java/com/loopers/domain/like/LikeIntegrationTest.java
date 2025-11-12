package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandDomainCreateRequest;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * LikeFacade 통합 테스트
 *
 * 좋아요 등록 및 취소 기능을 파사드 레벨에서 검증합니다.
 * 각 도메인 서비스를 통해 트랜잭션이 적용된 데이터 저장을 수행합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
@SpringBootTest
@DisplayName("LikeFacade 통합 테스트")
public class LikeIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;


    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * 좋아요 등록 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 등록")
    class 좋아요_등록 {

        /**
         * 유효한 사용자와 상품이 존재할 때 좋아요 등록에 성공하는지 테스트
         */
        @Test
        @DisplayName("유효한 사용자와 상품이면 좋아요 등록에 성공한다")
        void 유효한_사용자와_상품이면_좋아요_등록에_성공한다() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 브랜드 생성 요청
            BrandDomainCreateRequest brandRequest = BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명");
            BrandEntity savedBrand = brandService.registerBrand(brandRequest);

            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    savedBrand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When: 좋아요 등록
            LikeInfo result = likeFacade.upsertLike(userInfo.username(), savedProduct.getId());

            // Then: 좋아요 등록 성공 검증
            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo(userInfo.username());
            assertThat(result.productId()).isEqualTo(savedProduct.getId());
        }
    }


    /**
     * 좋아요 취소 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 취소")
    class 좋아요_취소 {
        // TODO: 좋아요 취소 테스트 케이스 구현 예정
    }
}
