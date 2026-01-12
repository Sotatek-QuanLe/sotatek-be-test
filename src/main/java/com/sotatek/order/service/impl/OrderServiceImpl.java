package com.sotatek.order.service.impl;

import com.sotatek.order.client.MemberClient;
import com.sotatek.order.client.PaymentClient;
import com.sotatek.order.client.ProductClient;
import com.sotatek.order.exception.*;
import com.sotatek.order.model.dto.external.MemberResponse;
import com.sotatek.order.model.dto.external.PaymentRequest;
import com.sotatek.order.model.dto.external.PaymentResponse;
import com.sotatek.order.model.dto.external.ProductResponse;
import com.sotatek.order.model.dto.external.ProductStockResponse;
import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.dto.response.OrderItemResponse;
import com.sotatek.order.model.dto.response.OrderResponse;
import com.sotatek.order.model.entity.Order;
import com.sotatek.order.model.entity.OrderItem;
import com.sotatek.order.model.enums.OrderStatus;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.lang.NonNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MemberClient memberClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    @Override
    @Transactional
    @SuppressWarnings("null")
    public OrderResponse createOrder(@NonNull CreateOrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberId());

        // TODO: Use CompletableFuture.allOf() for parallel validation in production
        // 1. Validate Member
        MemberResponse member = memberClient.getMember(request.getMemberId());
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new MemberInactiveException("Member status is not ACTIVE: " + member.getStatus());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .memberId(request.getMemberId())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .build();

        // 2. Validate Products and calculate total
        for (var itemRequest : request.getItems()) {
            String productId = itemRequest.getProductId();

            // Get product info
            ProductResponse product = productClient.getProduct(productId);
            if (!"AVAILABLE".equals(product.getStatus())) {
                throw new ProductUnavailableException("Product is not available: " + productId);
            }

            // Check stock
            ProductStockResponse stock = productClient.getStock(productId);
            if (stock.getAvailableQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + productId);
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 3. Process Payment
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(savedOrder.getId())
                .amount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .build();

        PaymentResponse paymentResponse = paymentClient.createPayment(paymentRequest);
        if ("COMPLETED".equals(paymentResponse.getStatus())) {
            log.info("Payment completed: orderId={}, transactionId={}", savedOrder.getId(),
                    paymentResponse.getTransactionId());
            savedOrder.setPaymentTransactionId(paymentResponse.getTransactionId());
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder = orderRepository.save(savedOrder);
        } else {
            log.warn("Payment failed for order: {}. Status remains PENDING", savedOrder.getId());
        }

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(@NonNull Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(@NonNull Long id, @NonNull UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Cannot update status of a CANCELLED order");
        }

        if (request.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Only CANCELLED status is allowed for this endpoint");
        }

        log.info("Cancelling order {}", id);
        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        return mapToResponse(updatedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .memberId(order.getMemberId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
