package com.sotatek.order.model.dto.external;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String status; // AVAILABLE, OUT_OF_STOCK, DISCONTINUED
}
