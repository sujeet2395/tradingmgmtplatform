package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.OrderCreateRequest;
import com.learn.tradingmgmt.dto.response.OrderResponse;
import com.learn.tradingmgmt.entity.Customer;
import com.learn.tradingmgmt.entity.Order;
import com.learn.tradingmgmt.entity.ShareSecurity;
import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.entity.enums.OrderStatus;
import com.learn.tradingmgmt.entity.enums.TransactionStatus;
import com.learn.tradingmgmt.exception.InvalidStateTransitionException;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.CustomerRepository;
import com.learn.tradingmgmt.repository.OrderRepository;
import com.learn.tradingmgmt.repository.ShareSecurityRepository;
import com.learn.tradingmgmt.repository.TransactionRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.OrderService;
import com.learn.tradingmgmt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ShareSecurityRepository shareSecurityRepository;
    private final TransactionService transactionService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Order createOrder(OrderCreateRequest request, UUID actingUserId) {
        ShareSecurity security = shareSecurityRepository.findById(request.securityId())
                .orElseThrow(() -> new ResourceNotFoundException("ShareSecurity", request.securityId()));

        Customer buyer = resolveCustomer(request.buyerCustomerId());
        Customer seller = resolveCustomer(request.sellerCustomerId());

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(request.orderType());
        order.setBuyerCustomer(buyer);
        order.setSellerCustomer(seller);
        order.setSecurity(security);
        order.setQuantity(request.quantity());
        order.setPricePerShare(request.pricePerShare());
        order.setGrossAmount(request.quantity().multiply(request.pricePerShare()));
        order.setOrderDate(request.orderDate() != null ? request.orderDate() : LocalDate.now());
        order.setNotes(request.notes());
        order.setStatus(OrderStatus.ORDER_RECEIVED);
        order.setCreatedBy(actingUserId);
        order.setUpdatedBy(actingUserId);

        Order saved = orderRepository.save(order);
        auditLogService.record(actingUserId, "ORDER_CREATE", "Order", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Override
    public List<OrderResponse> listOrders(OrderStatus status) {
        // return status == null ? orderRepository.findAll().stream().peek(or -> { or.getSecurity();
        //      or.getBuyerCustomer();
        //      or.getSellerCustomer();}).toList() : orderRepository.findByStatus(status);

         List<Order> orders = status == null
            ? orderRepository.findAll()
            : orderRepository.findByStatus(status);
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Override
    @Transactional
    public Order verifyOrder(UUID id, UUID actingUserId) {
        Order order = getOrder(id);
        requireStatus(order, OrderStatus.ORDER_RECEIVED);
        order.setStatus(OrderStatus.ORDER_VERIFIED);
        order.setUpdatedBy(actingUserId);
        Order saved = orderRepository.save(order);
        auditLogService.record(actingUserId, "ORDER_VERIFY", "Order", id,
                OrderStatus.ORDER_RECEIVED, OrderStatus.ORDER_VERIFIED, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public Order confirmOrder(UUID id, UUID actingUserId) {
        Order order = getOrder(id);
        requireStatus(order, OrderStatus.ORDER_VERIFIED);

        order.setStatus(OrderStatus.ORDER_CONFIRMED);
        order.setUpdatedBy(actingUserId);
        Order savedOrder = orderRepository.save(order);

        // Create the settlement Transaction (starts DRAFT -> ORDER_CONFIRMED via
        // the validated transition, never set directly).
        Transaction transaction = new Transaction();
        transaction.setTransactionNumber(generateTransactionNumber());
        transaction.setOrder(savedOrder);
        transaction.setSecurity(savedOrder.getSecurity());
        transaction.setQuantity(savedOrder.getQuantity());
        transaction.setAgreedPrice(savedOrder.getPricePerShare());
        transaction.setTotalAmount(savedOrder.getGrossAmount());
        transaction.setCreatedBy(actingUserId);
        transaction.setUpdatedBy(actingUserId);
        Transaction savedTransaction = transactionRepository.save(transaction);

        transactionService.transition(savedTransaction.getId(), TransactionStatus.ORDER_CONFIRMED,
                actingUserId, "Order " + savedOrder.getOrderNumber() + " confirmed");

        auditLogService.record(actingUserId, "ORDER_CONFIRM", "Order", id,
                OrderStatus.ORDER_VERIFIED, OrderStatus.ORDER_CONFIRMED, null, "API");
        return savedOrder;
    }

    @Override
    @Transactional
    public Order rejectOrder(UUID id, String reason, UUID actingUserId) {
        Order order = getOrder(id);
        OrderStatus before = order.getStatus();
        order.setStatus(OrderStatus.ORDER_REJECTED);
        order.setUpdatedBy(actingUserId);
        Order saved = orderRepository.save(order);
        auditLogService.record(actingUserId, "ORDER_REJECT", "Order", id, before, OrderStatus.ORDER_REJECTED, reason, "API");
        return saved;
    }

    private Customer resolveCustomer(UUID customerId) {
        if (customerId == null) return null;
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "Order " + order.getId() + " is " + order.getStatus() + ", expected " + expected);
        }
    }

    private String generateOrderNumber() {
        long seq = orderRepository.count() + 1;
        return "ORD-" + Year.now() + "-" + String.format("%05d", seq);
    }

    private String generateTransactionNumber() {
        long seq = transactionRepository.count() + 1;
        return "TXN-" + Year.now() + "-" + String.format("%05d", seq);
    }
}
