package com.sotatek.order.model.dto.external;

import com.sotatek.order.model.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequest {
    private Long orderId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
}
