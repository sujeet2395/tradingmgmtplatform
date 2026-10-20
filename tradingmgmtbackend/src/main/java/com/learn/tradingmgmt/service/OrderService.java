package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.OrderCreateRequest;
import com.learn.tradingmgmt.dto.response.OrderResponse;
import com.learn.tradingmgmt.entity.Order;
import com.learn.tradingmgmt.entity.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderCreateRequest request, UUID actingUserId);
    Order getOrder(UUID id);
    List<OrderResponse> listOrders(OrderStatus status);
    Order verifyOrder(UUID id, UUID actingUserId);
    /** Confirms the order and creates its linked Transaction. */
    Order confirmOrder(UUID id, UUID actingUserId);
    Order rejectOrder(UUID id, String reason, UUID actingUserId);
}
