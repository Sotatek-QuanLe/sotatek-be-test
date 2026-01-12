package com.sotatek.order.model.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductStockResponse {
    private String productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
}
