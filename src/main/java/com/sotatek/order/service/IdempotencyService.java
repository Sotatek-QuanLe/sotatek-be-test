package com.sotatek.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sotatek.order.model.dto.response.OrderResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    // Atomic cache with TTL (10 mins) and size limit (1000) to prevent memory leak
    private final Cache<String, OrderResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /**
     * Gets a cached response or computes a new one atomically.
     * Fixes critical race condition issue.
     */
    public OrderResponse getOrCompute(String key, Supplier<OrderResponse> supplier) {
        if (key == null) {
            return supplier.get();
        }
        return cache.get(key, k -> supplier.get());
    }

    public Optional<OrderResponse> getResponse(String key) {
        if (key == null)
            return Optional.empty();
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    public void storeResponse(String key, OrderResponse response) {
        if (key != null && response != null) {
            cache.put(key, response);
        }
    }
}
