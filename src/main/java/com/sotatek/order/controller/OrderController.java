package com.sotatek.order.controller;

import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.dto.response.OrderResponse;
import com.sotatek.order.service.IdempotencyService;
import com.sotatek.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating, retrieving, and cancelling orders")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Validates member, products, stock and processes payment")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @NonNull @Valid @RequestBody CreateOrderRequest request) {

        // Atomic operation to fix race condition and return 201 for both states
        OrderResponse response = idempotencyService.getOrCompute(idempotencyKey,
                () -> orderService.createOrder(request));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable @NonNull Long id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List orders with pagination and sorting")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<OrderResponse> response = orderService.listOrders(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cancel an order", description = "Only PENDING/CONFIRMED orders can be cancelled")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull UpdateOrderRequest request) {
        OrderResponse response = orderService.cancelOrder(id, request);
        return ResponseEntity.ok(response);
    }
}
