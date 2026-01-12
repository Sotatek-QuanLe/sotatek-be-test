package com.sotatek.order.service;

import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface OrderService {
    OrderResponse createOrder(@NonNull CreateOrderRequest request);

    OrderResponse getOrder(@NonNull Long id);

    Page<OrderResponse> listOrders(@NonNull Pageable pageable);

    OrderResponse cancelOrder(@NonNull Long id, @NonNull UpdateOrderRequest request);
}
