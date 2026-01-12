package com.sotatek.order.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private String error; // ErrorCode as string
    private String message; // Human readable message
    private LocalDateTime timestamp;
    private String traceId;

    // Optional: validation errors detail
    private Map<String, String> fieldErrors;
}
