package com.sotatek.order.service;

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
    @Mock
    private MemberClient memberClient;
    @Mock
    private ProductClient productClient;
    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequest createRequest;
    private Order order;
    private MemberResponse activeMember;
    private ProductResponse availableProduct;
    private ProductStockResponse abundantStock;
    private PaymentResponse completedPayment;

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

        activeMember = MemberResponse.builder().id(1L).status("ACTIVE").build();
        availableProduct = ProductResponse.builder().id(1L).name("Mock Product").price(new BigDecimal("99.99"))
                .status("AVAILABLE").build();
        abundantStock = ProductStockResponse.builder().productId("P001").availableQuantity(100).build();
        completedPayment = PaymentResponse.builder().id(1L).status("COMPLETED").transactionId("TXN-123").build();
    }

    @Test
    void createOrder_Success() {
        when(memberClient.getMember(anyString())).thenReturn(activeMember);
        when(productClient.getProduct(anyString())).thenReturn(availableProduct);
        when(productClient.getStock(anyString())).thenReturn(abundantStock);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentClient.createPayment(any(PaymentRequest.class))).thenReturn(completedPayment);

        OrderResponse response = orderService.createOrder(createRequest);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_MemberInactive() {
        activeMember.setStatus("INACTIVE");
        when(memberClient.getMember(anyString())).thenReturn(activeMember);

        assertThrows(MemberInactiveException.class, () -> orderService.createOrder(createRequest));
    }

    @Test
    void createOrder_MemberNotFound() {
        when(memberClient.getMember(anyString())).thenReturn(null);

        assertThrows(MemberNotFoundException.class, () -> orderService.createOrder(createRequest));
    }

    @Test
    void createOrder_ProductUnavailable() {
        when(memberClient.getMember(anyString())).thenReturn(activeMember);
        availableProduct.setStatus("DISCONTINUED");
        when(productClient.getProduct(anyString())).thenReturn(availableProduct);

        assertThrows(ProductUnavailableException.class, () -> orderService.createOrder(createRequest));
    }

    @Test
    void createOrder_ProductNotFound() {
        when(memberClient.getMember(anyString())).thenReturn(activeMember);
        when(productClient.getProduct(anyString())).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(createRequest));
    }

    @Test
    void createOrder_InsufficientStock() {
        when(memberClient.getMember(anyString())).thenReturn(activeMember);
        when(productClient.getProduct(anyString())).thenReturn(availableProduct);
        abundantStock.setAvailableQuantity(1);
        when(productClient.getStock(anyString())).thenReturn(abundantStock);

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(createRequest));
    }

    @Test
    void createOrder_PaymentFailed() {
        when(memberClient.getMember(anyString())).thenReturn(activeMember);
        when(productClient.getProduct(anyString())).thenReturn(availableProduct);
        when(productClient.getStock(anyString())).thenReturn(abundantStock);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse failedPayment = PaymentResponse.builder().id(1L).status("FAILED").build();
        when(paymentClient.createPayment(any(PaymentRequest.class))).thenReturn(failedPayment);

        assertThrows(PaymentFailedException.class, () -> orderService.createOrder(createRequest));
        // Verify: 1st save (initial PENDING) + 2nd save (compensation PAYMENT_FAILED)
        verify(orderRepository, times(2)).save(any(Order.class));
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
    }

    @Test
    void listOrders_Empty() {
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<OrderResponse> response = orderService.listOrders(PageRequest.of(0, 10));
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
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
    }

    @Test
    void cancelOrder_OrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.CANCELLED);

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(1L, updateRequest));
    }
}
