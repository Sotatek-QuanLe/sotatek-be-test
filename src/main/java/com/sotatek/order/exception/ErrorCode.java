package com.sotatek.order.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBER_INACTIVE(HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_UNAVAILABLE(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;
}
