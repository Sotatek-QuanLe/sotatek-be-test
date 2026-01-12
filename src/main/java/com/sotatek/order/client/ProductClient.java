package com.sotatek.order.client;

import com.sotatek.order.model.dto.external.ProductResponse;
import com.sotatek.order.model.dto.external.ProductStockResponse;
import org.springframework.lang.NonNull;

public interface ProductClient {
    ProductResponse getProduct(@NonNull String productId);

    ProductStockResponse getStock(@NonNull String productId);
}
