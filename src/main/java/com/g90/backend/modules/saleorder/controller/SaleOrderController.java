package com.g90.backend.modules.saleorder.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.saleorder.dto.SaleOrderActionRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderActionResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderCancelRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderDetailResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderInvoiceCreateRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderListQuery;
import com.g90.backend.modules.saleorder.dto.SaleOrderListResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderStatusUpdateRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderTimelineResponseData;
import com.g90.backend.modules.saleorder.service.SaleOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sale-orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class SaleOrderController {

    private final SaleOrderService saleOrderService;

    @GetMapping
    public ApiResponse<SaleOrderListResponseData> getSaleOrders(@Valid @ModelAttribute SaleOrderListQuery query) {
        return ApiResponse.success("Sale order list fetched successfully", saleOrderService.getSaleOrders(query));
    }

    @GetMapping("/{saleOrderId}")
    public ApiResponse<SaleOrderDetailResponseData> getSaleOrder(@PathVariable String saleOrderId) {
        return ApiResponse.success("Sale order detail fetched successfully", saleOrderService.getSaleOrder(saleOrderId));
    }

    @GetMapping("/{saleOrderId}/timeline")
    public ApiResponse<SaleOrderTimelineResponseData> getTimeline(@PathVariable String saleOrderId) {
        return ApiResponse.success("Sale order timeline fetched successfully", saleOrderService.getTimeline(saleOrderId));
    }

    @PatchMapping("/{saleOrderId}/status")
    public ApiResponse<SaleOrderActionResponseData> updateStatus(
            @PathVariable String saleOrderId,
            @Valid @RequestBody SaleOrderStatusUpdateRequest request
    ) {
        return ApiResponse.success("Sale order status updated successfully", saleOrderService.updateStatus(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/reserve")
    public ApiResponse<SaleOrderActionResponseData> reserve(
            @PathVariable String saleOrderId,
            @RequestBody(required = false) @Valid SaleOrderActionRequest request
    ) {
        return ApiResponse.success("Sale order reserved successfully", saleOrderService.reserve(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/pick")
    public ApiResponse<SaleOrderActionResponseData> pick(
            @PathVariable String saleOrderId,
            @RequestBody(required = false) @Valid SaleOrderActionRequest request
    ) {
        return ApiResponse.success("Sale order picked successfully", saleOrderService.pick(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/dispatch")
    public ApiResponse<SaleOrderActionResponseData> dispatch(
            @PathVariable String saleOrderId,
            @RequestBody(required = false) @Valid SaleOrderActionRequest request
    ) {
        return ApiResponse.success("Sale order dispatched successfully", saleOrderService.dispatch(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/deliver")
    public ApiResponse<SaleOrderActionResponseData> deliver(
            @PathVariable String saleOrderId,
            @RequestBody(required = false) @Valid SaleOrderActionRequest request
    ) {
        return ApiResponse.success("Sale order delivered successfully", saleOrderService.deliver(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/complete")
    public ApiResponse<SaleOrderActionResponseData> complete(
            @PathVariable String saleOrderId,
            @RequestBody(required = false) @Valid SaleOrderActionRequest request
    ) {
        return ApiResponse.success("Sale order completed successfully", saleOrderService.complete(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/cancel")
    public ApiResponse<SaleOrderActionResponseData> cancel(
            @PathVariable String saleOrderId,
            @Valid @RequestBody SaleOrderCancelRequest request
    ) {
        return ApiResponse.success("Sale order cancellation processed successfully", saleOrderService.cancel(saleOrderId, request));
    }

    @PostMapping("/{saleOrderId}/invoices")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @PathVariable String saleOrderId,
            @Valid @RequestBody SaleOrderInvoiceCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created from sale order successfully", saleOrderService.createInvoice(saleOrderId, request)));
    }
}
