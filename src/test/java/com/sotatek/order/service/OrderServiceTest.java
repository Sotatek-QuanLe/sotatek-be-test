package com.sotatek.order.service;

import com.sotatek.order.exception.InvalidOrderStatusException;
import com.sotatek.order.exception.OrderNotFoundException;
import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.OrderItemRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.dto.response.OrderResponse;
import com.sotatek.order.model.entity.Order;
import com.sotatek.order.model.entity.OrderItem;
import com.sotatek.order.model.enums.OrderStatus;
import com.sotatek.order.model.enums.PaymentMethod;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequest createRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId("P001");
        itemRequest.setQuantity(2);

        createRequest = new CreateOrderRequest();
        createRequest.setMemberId("M001");
        createRequest.setItems(List.of(itemRequest));
        createRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItem orderItem = OrderItem.builder()
                .productId("P001")
                .productName("Mock Product P001")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .subtotal(new BigDecimal("199.98"))
                .build();

        order = Order.builder()
                .id(1L)
                .memberId("M001")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("199.98"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(Collections.singletonList(orderItem))
                .build();
        orderItem.setOrder(order);
    }

    @Test
    void createOrder_Success() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.createOrder(createRequest);

        assertNotNull(response);
        assertEquals("M001", response.getMemberId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("199.98"), response.getTotalAmount());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrder_Success() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("M001", response.getMemberId());
    }

    @Test
    void getOrder_NotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(1L));
    }

    @Test
    void listOrders_Success() {
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);

        Page<OrderResponse> response = orderService.listOrders(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("M001", response.getContent().get(0).getMemberId());
    }

    @Test
    void cancelOrder_Success() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.CANCELLED);

        OrderResponse response = orderService.cancelOrder(1L, updateRequest);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void cancelOrder_AlreadyCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.CANCELLED);

        assertThrows(InvalidOrderStatusException.class, () -> orderService.cancelOrder(1L, updateRequest));
    }

    @Test
    void cancelOrder_InvalidStatus() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.CONFIRMED);

        assertThrows(InvalidOrderStatusException.class, () -> orderService.cancelOrder(1L, updateRequest));
    }
}
