package com.sotatek.order.service;

import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

/**
 * Service interface for managing orders.
 */
public interface OrderService {

    /**
     * Creates a new order by validating member, products, stock and processing
     * payment.
     *
     * @param request the order creation request
     * @return the created order response
     * @throws MemberInactiveException     if the member is not active
     * @throws ProductUnavailableException if any product is not available
     * @throws InsufficientStockException  if any product has insufficient stock
     * @throws PaymentFailedException      if payment fails
     */
    OrderResponse createOrder(@NonNull CreateOrderRequest request);

    /**
     * Retrieves an order by its ID.
     *
     * @param id the order ID
     * @return the order response
     * @throws OrderNotFoundException if order is not found
     */
    OrderResponse getOrder(@NonNull Long id);

    /**
     * Lists all orders with pagination.
     *
     * @param pageable pagination information
     * @return a page of order responses
     */
    Page<OrderResponse> listOrders(@NonNull Pageable pageable);

    /**
     * Cancels an existing order.
     *
     * @param id      the order ID
     * @param request the update request (must contain CANCELLED status)
     * @return the updated order response
     * @throws InvalidOrderStatusException if order is already cancelled or request
     *                                     status is invalid
     */
    OrderResponse cancelOrder(@NonNull Long id, @NonNull UpdateOrderRequest request);
}
