package com.sotatek.order.client.impl;

import com.sotatek.order.client.PaymentClient;
import com.sotatek.order.model.dto.external.PaymentRequest;
import com.sotatek.order.model.dto.external.PaymentResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MockPaymentClient implements PaymentClient {

    @Override
    public PaymentResponse createPayment(@NonNull PaymentRequest request) {
        String status = request.getAmount().compareTo(new BigDecimal("10000")) > 0 ? "FAILED" : "COMPLETED";

        return PaymentResponse.builder()
                .id(1L)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(status)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
