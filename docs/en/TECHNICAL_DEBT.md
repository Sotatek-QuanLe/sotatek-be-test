# Technical Debt & Future Improvements

> Areas for improvement if time permits or when scaling to production.

---

## 🔴 Critical (Must-fix for Production)

### 1. Race Condition - Stock Check
- **Issue**: No locking mechanism between stock checking and order creation.
- **Impact**: Overselling potential during concurrent requests.
- **Solution**:
  - Implement pessimistic locking: `SELECT ... FOR UPDATE`
  - Or optimistic locking with `@Version`
  - Or atomic stock reservation within the Product Service

### 2. Distributed Transaction - No Compensation
- **Issue**: Payment fails/timeouts after the order is saved → inconsistent state.
- **Impact**: Order remains `PENDING`, money might have been deducted.
- **Solution**:
  - Saga pattern with compensation events
  - Or Outbox pattern
  - Idempotency key for safe retries

### 3. Database - H2 In-Memory
- **Issue**: `ddl-auto: create-drop` → data loss upon restart.
- **Impact**: Not viable for production.
- **Solution**:
  - Switch to PostgreSQL/MySQL
  - Use Flyway/Liquibase migrations
  - Set `ddl-auto: validate` for production

---

## 🟠 Major (Should-fix)

### 4. External Calls - No Resilience
- **Issue**: Synchronous blocking calls, no timeout/retry/circuit breaker configured.
- **Impact**: Single external service failure causes the entire order service to go down.
- **Solution**:
  ```java
  // Add Resilience4j
  @CircuitBreaker(name = "memberService", fallbackMethod = "fallback")
  @Retry(name = "memberService")
  @TimeLimiter(name = "memberService")
  ```

### 5. Entity - Lombok @Data
- **Issue**: `@Data` on Entity causes issues with lazy loading and bidirectional relationships ("N+1 queries", `StackOverflowError`).
- **Impact**: Performance degradation and stack overflow exceptions.
- **Solution**:
  ```java
  @Entity
  @Getter
  @Setter
  @NoArgsConstructor
  public class Order {
      // Override equals/hashCode manually using only ID
  }
  ```

### 6. Idempotency - Duplicate Orders
- **Issue**: Retry request can create duplicate orders.
- **Solution**:
  - Add `Idempotency-Key` header
  - Store and check the key before processing
  ```java
  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request);
  ```

### 7. Cancel Order - Missing Refund
- **Issue**: Canceling a `CONFIRMED` order does not trigger a refund.
- **Solution**:
  - Add `refundPayment()` in `PaymentClient`
  - Call refund before updating status to `CANCELLED`

---

## 🟡 Minor (Nice-to-have)

### 8. Pagination - Missing Sort
```java
// Current
listOrders(int page, int size)

// Should be
listOrders(int page, int size, String sortBy, String sortDir)
```

### 9. Error Response - Missing Trace ID
```java
@Data
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String traceId;  // ← Add this
}
```

### 10. Observability
- [ ] Add Micrometer metrics
- [ ] Add distributed tracing (Sleuth/OpenTelemetry)
- [ ] Structured logging with correlation ID

### 11. Security
- [ ] Authentication/Authorization (Spring Security + JWT)
- [ ] Rate limiting
- [ ] Input sanitization

---

## Implementation Priority

| Priority | Items | Effort |
|----------|-------|--------|
| P0 | #1, #2, #3 | High |
| P1 | #4, #5, #6, #7 | Medium |
| P2 | #8, #9, #10, #11 | Low |

---

## Notes

- Current implementation is **acceptable for assignment/PoC**
- P0 items are **blocking for production deployment**
- Review again when planning to scale or handle real transactions
