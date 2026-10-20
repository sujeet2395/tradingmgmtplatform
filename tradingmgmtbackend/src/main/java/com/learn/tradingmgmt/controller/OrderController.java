package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.OrderCreateRequest;
import com.learn.tradingmgmt.dto.response.OrderResponse;
import com.learn.tradingmgmt.entity.Order;
import com.learn.tradingmgmt.entity.enums.OrderStatus;
import com.learn.tradingmgmt.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderCreateRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, actingUserId));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status) {
        return orderService.listOrders(status);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/verify")
    public Order verify(@PathVariable UUID id,
                         @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return orderService.verifyOrder(id, actingUserId);
    }

    @PostMapping("/{id}/confirm")
    public Order confirm(@PathVariable UUID id,
                          @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return orderService.confirmOrder(id, actingUserId);
    }

    @PostMapping("/{id}/reject")
    public Order reject(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body,
                         @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        String reason = body != null ? body.get("reason") : null;
        return orderService.rejectOrder(id, reason, actingUserId);
    }
}
