# Technical Debt & Future Improvements

> Các điểm cần cải thiện nếu còn thời gian hoặc khi scale lên production.

---

## 🔴 Critical (Must-fix for Production)

### 1. Race Condition - Stock Check
- **Issue**: Không có locking giữa check stock và create order
- **Impact**: Overselling khi concurrent requests
- **Solution**:
  - Pessimistic lock: `SELECT ... FOR UPDATE`
  - Hoặc Optimistic lock với `@Version`
  - Hoặc atomic stock reservation trong Product Service

### 2. Distributed Transaction - No Compensation
- **Issue**: Payment fail/timeout sau khi order đã saved → inconsistent state
- **Impact**: Order stuck PENDING, tiền có thể đã bị trừ
- **Solution**:
  - Saga pattern với compensation events
  - Hoặc Outbox pattern
  - Idempotency key cho retry safety

### 3. Database - H2 In-Memory
- **Issue**: `ddl-auto: create-drop` → mất data khi restart
- **Impact**: Không thể dùng cho production
- **Solution**:
  - Switch sang PostgreSQL/MySQL
  - Sử dụng Flyway/Liquibase migrations
  - `ddl-auto: validate` cho production

---

## 🟠 Major (Should-fix)

### 4. External Calls - No Resilience
- **Issue**: Synchronous blocking calls, no timeout/retry/circuit breaker
- **Impact**: Single external service down → toàn bộ order service down
- **Solution**:
  ```java
  // Add Resilience4j
  @CircuitBreaker(name = "memberService", fallbackMethod = "fallback")
  @Retry(name = "memberService")
  @TimeLimiter(name = "memberService")
  ```

### 5. Entity - Lombok @Data
- **Issue**: `@Data` trên Entity gây issues với lazy loading và bidirectional relationships
- **Impact**: N+1 queries, StackOverflowError
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
- **Issue**: Retry request có thể tạo duplicate orders
- **Solution**:
  - Add `Idempotency-Key` header
  - Store và check key trước khi process
  ```java
  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request);
  ```

### 7. Cancel Order - Missing Refund
- **Issue**: Cancel CONFIRMED order không trigger refund
- **Solution**:
  - Add `refundPayment()` trong PaymentClient
  - Gọi refund trước khi update status CANCELLED

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
- [ ] Structured logging với correlation ID

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

- Current implementation là **acceptable cho assignment/PoC**
- Các items P0 là **blocking cho production deployment**
- Review lại khi có plan scale hoặc handle real transactions
