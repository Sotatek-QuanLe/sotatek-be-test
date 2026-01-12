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
import com.sotatek.order.model.enums.ExternalStatus;

import org.springframework.lang.NonNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Transactional(timeout = 10) // Issue 9: Prevent forever locks
    @SuppressWarnings("null")
    public OrderResponse createOrder(@NonNull CreateOrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberId());

        // 1. Validate Member (Issue 2: Defensive Coding, Issue 4: Magic Strings)
        validateMember(request.getMemberId());

        // 2. Validate Products and calculate total (Issue 8: Rounding)
        Order order = buildOrderEntity(request);
        Order savedOrder = orderRepository.save(order);

        // 3. Process Payment with compensation logic
        try {
            processPayment(savedOrder);
        } catch (PaymentFailedException e) {
            log.warn("Payment failed, marking order {} as PAYMENT_FAILED", savedOrder.getId());
            savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(savedOrder);
            throw e;
        }

        return mapToResponse(savedOrder);
    }

    @SuppressWarnings("null")
    private void validateMember(String memberId) {
        MemberResponse member = memberClient.getMember(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Member service returned null for id: " + memberId);
        }
        if (!ExternalStatus.Member.ACTIVE.getValue().equals(member.getStatus())) {
            throw new MemberInactiveException("Member status is not ACTIVE: " + member.getStatus());
        }
    }

    @SuppressWarnings("null")
    private Order buildOrderEntity(CreateOrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .memberId(request.getMemberId())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .build();

        for (var itemRequest : request.getItems()) {
            String productId = itemRequest.getProductId();
            ProductResponse product = productClient.getProduct(productId);

            if (product == null) {
                throw new ProductNotFoundException("Product service returned null for id: " + productId);
            }
            if (!ExternalStatus.Product.AVAILABLE.getValue().equals(product.getStatus())) {
                throw new ProductUnavailableException("Product is not available: " + productId);
            }

            ProductStockResponse stock = productClient.getStock(productId);
            if (stock == null || stock.getAvailableQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + productId);
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP))
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        return order;
    }

    @SuppressWarnings("null")
    private void processPayment(Order order) {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .build();

        PaymentResponse paymentResponse = paymentClient.createPayment(paymentRequest);

        if (paymentResponse == null) {
            throw new PaymentFailedException("Payment service returned null for order: " + order.getId());
        }

        // CRITICAL: Log and store transaction ID before status update (Issue 1)
        if (ExternalStatus.Payment.COMPLETED.getValue().equals(paymentResponse.getStatus())) {
            log.info("Payment completed: orderId={}, transactionId={}", order.getId(),
                    paymentResponse.getTransactionId());
            order.setPaymentTransactionId(paymentResponse.getTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } else {
            log.error("Payment failed: orderId={}, status={}, message={}",
                    order.getId(), paymentResponse.getStatus(), "Unknown failure");
            throw new PaymentFailedException("Payment failed with status: " + paymentResponse.getStatus());
        }
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
    @SuppressWarnings("null")
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

        // Phase 8: Refund confirmed order with idempotency check (Issue 4)
        if (order.getStatus() == OrderStatus.CONFIRMED && order.getPaymentTransactionId() != null) {
            if (order.getRefundTransactionId() != null) {
                log.info("Order {} already has a refund transaction: {}", id, order.getRefundTransactionId());
            } else {
                log.info("Triggering refund for order {}, transaction {}", id, order.getPaymentTransactionId());
                PaymentResponse refundResponse = paymentClient.refundPayment(order.getPaymentTransactionId(),
                        order.getTotalAmount());

                if (refundResponse != null
                        && ExternalStatus.Payment.REFUNDED.getValue().equals(refundResponse.getStatus())) {
                    order.setRefundTransactionId(refundResponse.getTransactionId());
                } else {
                    log.warn("Refund process returned non-success status for order {}: {}", id,
                            refundResponse != null ? refundResponse.getStatus() : "null");
                }
            }
        }

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
