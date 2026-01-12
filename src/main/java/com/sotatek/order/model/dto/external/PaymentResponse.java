package com.sotatek.order.model.dto.external;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private String transactionId;
    private LocalDateTime createdAt;
}
