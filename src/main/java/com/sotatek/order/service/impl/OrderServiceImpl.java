package com.sotatek.order.service.impl;

import com.sotatek.order.exception.InvalidOrderStatusException;
import com.sotatek.order.exception.OrderNotFoundException;
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

    @Override
    @Transactional
    public OrderResponse createOrder(@NonNull CreateOrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .memberId(request.getMemberId())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .build();

        for (var itemRequest : request.getItems()) {
            // Hardcoded product info for Phase 2
            BigDecimal unitPrice = new BigDecimal("99.99");
            String productName = "Mock Product " + itemRequest.getProductId();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(productName)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

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
