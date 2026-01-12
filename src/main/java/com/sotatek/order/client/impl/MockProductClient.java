package com.sotatek.order.client.impl;

import com.sotatek.order.client.ProductClient;
import com.sotatek.order.exception.ProductNotFoundException;
import com.sotatek.order.model.dto.external.ProductResponse;
import com.sotatek.order.model.dto.external.ProductStockResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.sotatek.order.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockProductClient implements ProductClient {

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "productFallback")
    @Retry(name = "productService")
    public ProductResponse getProduct(@NonNull String productId) {
        if ("not-found".equals(productId)) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        String status = "discontinued".equals(productId) ? "DISCONTINUED" : "AVAILABLE";

        return ProductResponse.builder()
                .id(1L)
                .name("Mock Product " + productId)
                .price(new BigDecimal("99.99"))
                .status(status)
                .build();
    }

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "stockFallback")
    @Retry(name = "productService")
    public ProductStockResponse getStock(@NonNull String productId) {
        int availableQuantity = "out-of-stock".equals(productId) ? 0 : 100;

        return ProductStockResponse.builder()
                .productId(productId)
                .quantity(100)
                .reservedQuantity(0)
                .availableQuantity(availableQuantity)
                .build();
    }

    public ProductResponse productFallback(String productId, Throwable t) {
        log.error("Product service fallback for id: {}, error: {}", productId, t.getMessage());
        throw new ServiceUnavailableException("Product service is temporarily unavailable: " + t.getMessage());
    }

    public ProductStockResponse stockFallback(String productId, Throwable t) {
        log.error("Stock service fallback for id: {}, error: {}", productId, t.getMessage());
        throw new ServiceUnavailableException("Stock service is temporarily unavailable: " + t.getMessage());
    }
}
