package com.sotatek.order.model.dto.request;

import com.sotatek.order.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status; // Usually only for CANCELLED in Phase 2
}
