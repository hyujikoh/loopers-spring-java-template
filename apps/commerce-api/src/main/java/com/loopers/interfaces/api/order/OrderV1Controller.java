package com.loopers.interfaces.api.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderSummary;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final UserFacade userFacade;

    @PostMapping(Uris.Order.CREATE)
    @Override
    public ApiResponse<OrderV1Dtos.OrderCreateResponse> createOrder(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody OrderV1Dtos.OrderCreateRequest request
    ) {
        OrderCreateCommand command = request.toCommand(username);

        // 결제 타입에 따라 분기
        OrderInfo orderInfo;
        if ("POINT".equals(command.paymentType())) {
            // 포인트 결제: 즉시 결제 및 주문 확정
            orderInfo = orderFacade.createOrderByPoint(command);
        } else if ("CARD".equals(command.paymentType())) {
            // 카드 결제: 주문만 생성 (결제는 별도 API로 처리)
            orderInfo = orderFacade.createOrderForCardPayment(command);
        } else {
            throw new IllegalArgumentException("지원하지 않는 결제 타입입니다: " + command.paymentType());
        }

        OrderV1Dtos.OrderCreateResponse response = OrderV1Dtos.OrderCreateResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    @GetMapping(Uris.Order.GET_LIST)
    @Override
    public ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>> getOrders(
            @RequestHeader("X-USER-ID") String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserInfo userInfo = userFacade.getUserByUsername(username);
        Page<OrderSummary> orders = orderFacade.getOrderSummariesByUserId(userInfo.id(), pageable);
        Page<OrderV1Dtos.OrderListResponse> responsePage = orders.map(summary ->
                new OrderV1Dtos.OrderListResponse(
                        summary.id(),
                        summary.status(),
                        summary.originalTotalAmount(),
                        summary.discountAmount(),
                        summary.finalTotalAmount(),
                        summary.createdAt()
                )
        );
        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping(Uris.Order.GET_DETAIL)
    @Override
    public ApiResponse<OrderV1Dtos.OrderDetailResponse> getOrderDetail(
            @RequestHeader("X-USER-ID") String username,
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrderById(username, orderId);
        OrderV1Dtos.OrderDetailResponse response = OrderV1Dtos.OrderDetailResponse.from(orderInfo);
        return ApiResponse.success(response);
    }
}
