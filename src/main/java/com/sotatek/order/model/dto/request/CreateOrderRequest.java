package com.sotatek.order.model.dto.request;

import com.sotatek.order.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 50, message = "Order cannot exceed 50 items")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
