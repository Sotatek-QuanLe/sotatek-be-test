package com.sotatek.order.exception;

import com.sotatek.order.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return buildErrorResponse(ErrorCode.ORDER_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex) {
        return buildErrorResponse(ErrorCode.MEMBER_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return buildErrorResponse(ErrorCode.PRODUCT_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MemberInactiveException.class)
    public ResponseEntity<ErrorResponse> handleMemberInactive(MemberInactiveException ex) {
        return buildErrorResponse(ErrorCode.MEMBER_INACTIVE, ex.getMessage());
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProductUnavailable(ProductUnavailableException ex) {
        return buildErrorResponse(ErrorCode.PRODUCT_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return buildErrorResponse(ErrorCode.INSUFFICIENT_STOCK, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatus(InvalidOrderStatusException ex) {
        return buildErrorResponse(ErrorCode.INVALID_ORDER_STATUS, ex.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
        return buildErrorResponse(ErrorCode.PAYMENT_FAILED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorCode.VALIDATION_ERROR.name())
                .message("Validation failed")
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation error: {}", fieldErrors);
        return new ResponseEntity<>(errorResponse, ErrorCode.VALIDATION_ERROR.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        log.error("Internal server error: ", ex);
        return buildErrorResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorCode.name())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        HttpStatus status = errorCode.getStatus();
        if (status.is5xxServerError()) {
            log.error("{}: {}", errorCode, message);
        } else {
            log.warn("{}: {}", errorCode, message);
        }

        return new ResponseEntity<>(errorResponse, status);
    }
}
