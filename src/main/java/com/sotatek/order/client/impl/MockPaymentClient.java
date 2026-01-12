package com.sotatek.order.client.impl;

import com.sotatek.order.client.PaymentClient;
import com.sotatek.order.model.dto.external.PaymentRequest;
import com.sotatek.order.model.dto.external.PaymentResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.sotatek.order.exception.ServiceUnavailableException;
import com.sotatek.order.model.enums.ExternalStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockPaymentClient implements PaymentClient {

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse createPayment(@NonNull PaymentRequest request) {
        String status = request.getAmount().compareTo(new BigDecimal("10000")) > 0
                ? ExternalStatus.Payment.FAILED.getValue()
                : ExternalStatus.Payment.COMPLETED.getValue();

        return PaymentResponse.builder()
                .id(1L)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(status)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundFallback")
    @Retry(name = "paymentService")
    public PaymentResponse refundPayment(@NonNull String transactionId, @NonNull BigDecimal amount) {
        return PaymentResponse.builder()
                .id(2L)
                .amount(amount)
                .status(ExternalStatus.Payment.REFUNDED.getValue())
                .transactionId(transactionId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public PaymentResponse paymentFallback(PaymentRequest request, Throwable t) {
        log.error("Payment service fallback for order: {}, error: {}", request.getOrderId(), t.getMessage());
        throw new ServiceUnavailableException("Payment service is temporarily unavailable: " + t.getMessage());
    }

    public PaymentResponse refundFallback(String transactionId, BigDecimal amount, Throwable t) {
        log.error("Refund service fallback for transaction: {}, error: {}", transactionId, t.getMessage());
        throw new ServiceUnavailableException("Refund service is temporarily unavailable: " + t.getMessage());
    }
}
