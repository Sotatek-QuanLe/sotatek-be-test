package com.sotatek.order.service;

import com.sotatek.order.model.dto.response.OrderResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    // Simple in-memory store for demo. In production, use Redis with TTL.
    private final Map<String, OrderResponse> store = new ConcurrentHashMap<>();

    public Optional<OrderResponse> getResponse(String key) {
        if (key == null)
            return Optional.empty();
        return Optional.ofNullable(store.get(key));
    }

    public void storeResponse(String key, OrderResponse response) {
        if (key != null && response != null) {
            store.put(key, response);
        }
    }
}
