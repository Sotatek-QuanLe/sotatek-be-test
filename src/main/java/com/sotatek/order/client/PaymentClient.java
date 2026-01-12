package com.sotatek.order.client;

import com.sotatek.order.model.dto.external.PaymentRequest;
import com.sotatek.order.model.dto.external.PaymentResponse;
import org.springframework.lang.NonNull;

public interface PaymentClient {
    PaymentResponse createPayment(@NonNull PaymentRequest request);

    PaymentResponse refundPayment(@NonNull String transactionId, @NonNull java.math.BigDecimal amount);
}
