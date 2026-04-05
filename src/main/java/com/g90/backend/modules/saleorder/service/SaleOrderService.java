package com.g90.backend.modules.saleorder.service;

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
import java.math.BigDecimal;

public interface SaleOrderService {

    SaleOrderListResponseData getSaleOrders(SaleOrderListQuery query);

    SaleOrderDetailResponseData getSaleOrder(String saleOrderId);

    SaleOrderTimelineResponseData getTimeline(String saleOrderId);

    SaleOrderActionResponseData updateStatus(String saleOrderId, SaleOrderStatusUpdateRequest request);

    SaleOrderActionResponseData reserve(String saleOrderId, SaleOrderActionRequest request);

    SaleOrderActionResponseData pick(String saleOrderId, SaleOrderActionRequest request);

    SaleOrderActionResponseData dispatch(String saleOrderId, SaleOrderActionRequest request);

    SaleOrderActionResponseData deliver(String saleOrderId, SaleOrderActionRequest request);

    SaleOrderActionResponseData complete(String saleOrderId, SaleOrderActionRequest request);

    SaleOrderActionResponseData cancel(String saleOrderId, SaleOrderCancelRequest request);

    InvoiceResponse createInvoice(String saleOrderId, SaleOrderInvoiceCreateRequest request);

    void registerInventoryIssue(String saleOrderId, String productId, BigDecimal quantity, String note, String userId);
}
