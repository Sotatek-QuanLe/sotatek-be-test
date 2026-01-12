# Master Implementation Plan

> **Mục đích**: Hướng dẫn AI Agent implement từng phase một cách độc lập và có kiểm chứng.

---

## 📋 Overview

| Phase | Tên | Thời gian | Mục tiêu chính |
|-------|-----|-----------|----------------|
| 1 | Project Foundation | 30 phút | Setup project, entities, database |
| 2 | Core Order APIs | 60 phút | CRUD endpoints hoàn chỉnh |
| 3 | External Service Integration | 45 phút | Mock clients + validation flow |
| 4 | Error Handling & Validation | 30 phút | Global exception handler, input validation |
| 5 | Unit Testing | 30 phút | Service layer tests |
| 6 | Documentation & Polish | 15 phút | Swagger, README, cleanup |

**Tổng thời gian dự kiến**: ~3.5 giờ (buffer 30 phút cho debug)

---

## Phase 1: Project Foundation

### 🎯 Mục tiêu
Setup nền tảng dự án với đầy đủ dependencies, entity models, và cấu hình database.

### 📦 Output mong muốn

```
src/main/java/com/sotatek/order/
├── OrderApplication.java (đã có)
├── model/
│   ├── entity/
│   │   ├── Order.java
│   │   └── OrderItem.java
│   └── enums/
│       ├── OrderStatus.java
│       └── PaymentMethod.java
├── repository/
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
└── config/
    └── JpaAuditingConfig.java

src/main/resources/
└── application.yml (updated với H2 config)
```

### 📝 Tasks chi tiết

1. **Update `build.gradle`** - Thêm dependencies:
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-validation`
   - `h2` (runtime)
   - `lombok` (compileOnly + annotationProcessor)
   - `springdoc-openapi-starter-webmvc-ui:2.3.0`

2. **Create `OrderStatus.java`** enum:
   ```java
   public enum OrderStatus {
       PENDING, CONFIRMED, CANCELLED
   }
   ```

3. **Create `PaymentMethod.java`** enum:
   ```java
   public enum PaymentMethod {
       CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER
   }
   ```

4. **Create `Order.java`** entity:
   - Fields: `id`, `memberId`, `status`, `totalAmount`, `paymentMethod`, `createdAt`, `updatedAt`
   - Relationship: `@OneToMany` với `OrderItem`
   - Annotations: `@Entity`, `@Data`, `@EntityListeners(AuditingEntityListener.class)`

5. **Create `OrderItem.java`** entity:
   - Fields: `id`, `productId`, `productName`, `quantity`, `unitPrice`, `subtotal`
   - Relationship: `@ManyToOne` với `Order`

6. **Create repositories**:
   - `OrderRepository extends JpaRepository<Order, Long>`
   - `OrderItemRepository extends JpaRepository<OrderItem, Long>`

7. **Create `JpaAuditingConfig.java`**:
   - Enable `@EnableJpaAuditing`

8. **Update `application.yml`**:
   ```yaml
   spring:
     application:
       name: order-service
     datasource:
       url: jdbc:h2:mem:orderdb
       driver-class-name: org.h2.Driver
       username: sa
       password:
     jpa:
       hibernate:
         ddl-auto: create-drop
       show-sql: true
     h2:
       console:
         enabled: true
         path: /h2-console
   server:
     port: 8080
   ```

### ✅ Tiêu chí DONE

- [ ] `./gradlew build` pass không lỗi
- [ ] `./gradlew bootRun` khởi động thành công
- [ ] Truy cập `http://localhost:8080/h2-console` thấy tables `orders` và `order_items`
- [ ] Không có warning về missing dependencies

---

## Phase 2: Core Order APIs

### 🎯 Mục tiêu
Implement đầy đủ CRUD operations cho Order (chưa có external validation).

### 📦 Output mong muốn

```
src/main/java/com/sotatek/order/
├── controller/
│   └── OrderController.java
├── service/
│   ├── OrderService.java (interface)
│   └── impl/
│       └── OrderServiceImpl.java
├── model/
│   └── dto/
│       ├── request/
│       │   ├── CreateOrderRequest.java
│       │   ├── OrderItemRequest.java
│       │   └── UpdateOrderRequest.java
│       └── response/
│           ├── OrderResponse.java
│           └── OrderItemResponse.java
└── exception/
    ├── OrderNotFoundException.java
    └── InvalidOrderStatusException.java
```

### 📝 Tasks chi tiết

1. **Create Request DTOs**:

   `CreateOrderRequest.java`:
   ```java
   @Data
   public class CreateOrderRequest {
       @NotBlank
       private String memberId;
       
       @NotEmpty
       @Valid
       private List<OrderItemRequest> items;
       
       @NotNull
       private PaymentMethod paymentMethod;
   }
   ```

   `OrderItemRequest.java`:
   ```java
   @Data
   public class OrderItemRequest {
       @NotBlank
       private String productId;
       
       @NotNull
       @Min(1)
       private Integer quantity;
   }
   ```

   `UpdateOrderRequest.java`:
   ```java
   @Data
   public class UpdateOrderRequest {
       @NotNull
       private OrderStatus status;  // Chỉ cho phép CANCELLED
   }
   ```

2. **Create Response DTOs**:

   `OrderResponse.java`:
   ```java
   @Data
   @Builder
   public class OrderResponse {
       private Long id;
       private String memberId;
       private List<OrderItemResponse> items;
       private BigDecimal totalAmount;
       private OrderStatus status;
       private PaymentMethod paymentMethod;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
   }
   ```

   `OrderItemResponse.java`:
   ```java
   @Data
   @Builder
   public class OrderItemResponse {
       private String productId;
       private String productName;
       private Integer quantity;
       private BigDecimal unitPrice;
       private BigDecimal subtotal;
   }
   ```

3. **Create custom exceptions**:
   - `OrderNotFoundException extends RuntimeException`
   - `InvalidOrderStatusException extends RuntimeException`

4. **Create `OrderService` interface**:
   ```java
   public interface OrderService {
       OrderResponse createOrder(CreateOrderRequest request);
       OrderResponse getOrder(Long id);
       Page<OrderResponse> listOrders(Pageable pageable);
       OrderResponse cancelOrder(Long id);
   }
   ```

5. **Create `OrderServiceImpl`**:
   - Implement tất cả methods
   - Tạm thời hardcode product info (sẽ thay bằng external call ở Phase 3)
   - Business logic:
     - `createOrder`: Tạo order với status `PENDING`, tính `totalAmount`
     - `getOrder`: Tìm theo ID, throw `OrderNotFoundException` nếu không có
     - `listOrders`: Return `Page<OrderResponse>`
     - `cancelOrder`: Chỉ cho phép cancel nếu status != `CANCELLED`

6. **Create `OrderController`**:
   ```java
   @RestController
   @RequestMapping("/api/orders")
   @RequiredArgsConstructor
   public class OrderController {
       
       @PostMapping
       public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request);
       
       @GetMapping("/{id}")
       public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id);
       
       @GetMapping
       public ResponseEntity<Page<OrderResponse>> listOrders(
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "10") int size);
       
       @PutMapping("/{id}")
       public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request);
   }
   ```

### ✅ Tiêu chí DONE

- [ ] `./gradlew build` pass
- [ ] POST `/api/orders` trả về 201 với order data
- [ ] GET `/api/orders/{id}` trả về order đã tạo
- [ ] GET `/api/orders?page=0&size=10` trả về paginated list
- [ ] PUT `/api/orders/{id}` với `{"status": "CANCELLED"}` hoạt động
- [ ] GET order không tồn tại trả về 404

**Test commands**:
```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": "M001",
    "items": [{"productId": "P001", "quantity": 2}],
    "paymentMethod": "CREDIT_CARD"
  }'

# Get order
curl http://localhost:8080/api/orders/1

# List orders
curl "http://localhost:8080/api/orders?page=0&size=10"

# Cancel order
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "CANCELLED"}'
```

---

## Phase 3: External Service Integration

### 🎯 Mục tiêu
Implement mock clients cho Member, Product, Payment services và tích hợp vào order flow.

### 📦 Output mong muốn

```
src/main/java/com/sotatek/order/
├── client/
│   ├── MemberClient.java (interface)
│   ├── ProductClient.java (interface)
│   ├── PaymentClient.java (interface)
│   └── impl/
│       ├── MockMemberClient.java
│       ├── MockProductClient.java
│       └── MockPaymentClient.java
├── model/
│   └── dto/
│       └── external/
│           ├── MemberResponse.java
│           ├── ProductResponse.java
│           ├── ProductStockResponse.java
│           ├── PaymentRequest.java
│           └── PaymentResponse.java
└── exception/
    ├── MemberNotFoundException.java
    ├── MemberInactiveException.java
    ├── ProductNotFoundException.java
    ├── InsufficientStockException.java
    ├── ProductUnavailableException.java
    └── PaymentFailedException.java
```

### 📝 Tasks chi tiết

1. **Create External DTOs**:

   `MemberResponse.java`:
   ```java
   @Data
   @Builder
   public class MemberResponse {
       private Long id;
       private String name;
       private String email;
       private String status;  // ACTIVE, INACTIVE, SUSPENDED
       private String grade;   // BRONZE, SILVER, GOLD, PLATINUM
   }
   ```

   `ProductResponse.java`:
   ```java
   @Data
   @Builder
   public class ProductResponse {
       private Long id;
       private String name;
       private BigDecimal price;
       private String status;  // AVAILABLE, OUT_OF_STOCK, DISCONTINUED
   }
   ```

   `ProductStockResponse.java`:
   ```java
   @Data
   @Builder
   public class ProductStockResponse {
       private Long productId;
       private Integer quantity;
       private Integer reservedQuantity;
       private Integer availableQuantity;
   }
   ```

   `PaymentRequest.java`:
   ```java
   @Data
   @Builder
   public class PaymentRequest {
       private Long orderId;
       private BigDecimal amount;
       private PaymentMethod paymentMethod;
   }
   ```

   `PaymentResponse.java`:
   ```java
   @Data
   @Builder
   public class PaymentResponse {
       private Long id;
       private Long orderId;
       private BigDecimal amount;
       private String status;  // PENDING, COMPLETED, FAILED, REFUNDED
       private String transactionId;
       private LocalDateTime createdAt;
   }
   ```

2. **Create Client Interfaces**:

   ```java
   public interface MemberClient {
       MemberResponse getMember(String memberId);
   }
   
   public interface ProductClient {
       ProductResponse getProduct(String productId);
       ProductStockResponse getStock(String productId);
   }
   
   public interface PaymentClient {
       PaymentResponse createPayment(PaymentRequest request);
   }
   ```

3. **Create Mock Implementations** (theo bảng behavior trong ASSUMPTIONS.md):

   `MockMemberClient.java`:
   - `memberId = "not-found"` → throw `MemberNotFoundException`
   - `memberId = "inactive-member"` → return member with status `INACTIVE`
   - Else → return member with status `ACTIVE`

   `MockProductClient.java`:
   - `productId = "not-found"` → throw `ProductNotFoundException`
   - `productId = "out-of-stock"` → return stock with `availableQuantity = 0`
   - `productId = "discontinued"` → return product with status `DISCONTINUED`
   - Else → return product with `status = AVAILABLE`, `price = 99.99`, `stock = 100`

   `MockPaymentClient.java`:
   - `amount > 10000` → return payment with status `FAILED`
   - Else → return payment with status `COMPLETED`

4. **Create custom exceptions** cho external services

5. **Update `OrderServiceImpl`**:
   - Inject `MemberClient`, `ProductClient`, `PaymentClient`
   - Trong `createOrder`:
     1. Validate member (exists + ACTIVE)
     2. Validate each product (exists + AVAILABLE + has stock)
     3. Calculate `totalAmount` từ product prices
     4. Save order với status `PENDING`
     5. Process payment
     6. Update status to `CONFIRMED` if payment success

### ✅ Tiêu chí DONE

- [ ] Create order với member hợp lệ → success
- [ ] Create order với `memberId = "not-found"` → 404 error
- [ ] Create order với `memberId = "inactive-member"` → 400 error
- [ ] Create order với `productId = "not-found"` → 404 error
- [ ] Create order với `productId = "out-of-stock"` → 400 error
- [ ] Create order với `totalAmount > 10000` → payment failed, order stays `PENDING`
- [ ] Logging hiển thị các bước validation

**Test commands**:
```bash
# Invalid member
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"memberId": "not-found", "items": [{"productId": "P001", "quantity": 1}], "paymentMethod": "CREDIT_CARD"}'

# Out of stock product
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"memberId": "M001", "items": [{"productId": "out-of-stock", "quantity": 1}], "paymentMethod": "CREDIT_CARD"}'
```

---

## Phase 4: Error Handling & Validation

### 🎯 Mục tiêu
Implement global exception handler và chuẩn hóa error response format.

### 📦 Output mong muốn

```
src/main/java/com/sotatek/order/
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ErrorCode.java (enum)
└── model/
    └── dto/
        └── response/
            └── ErrorResponse.java
```

### 📝 Tasks chi tiết

1. **Create `ErrorCode.java`** enum:
   ```java
   public enum ErrorCode {
       ORDER_NOT_FOUND,
       MEMBER_NOT_FOUND,
       MEMBER_INACTIVE,
       PRODUCT_NOT_FOUND,
       PRODUCT_UNAVAILABLE,
       INSUFFICIENT_STOCK,
       INVALID_ORDER_STATUS,
       PAYMENT_FAILED,
       VALIDATION_ERROR,
       INTERNAL_ERROR
   }
   ```

2. **Create `ErrorResponse.java`**:
   ```java
   @Data
   @Builder
   public class ErrorResponse {
       private String error;      // ErrorCode as string
       private String message;    // Human readable message
       private LocalDateTime timestamp;
       
       // Optional: validation errors detail
       private Map<String, String> fieldErrors;
   }
   ```

3. **Create `GlobalExceptionHandler.java`**:
   ```java
   @RestControllerAdvice
   @Slf4j
   public class GlobalExceptionHandler {
       
       @ExceptionHandler(OrderNotFoundException.class)
       public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex);
       
       @ExceptionHandler(MemberNotFoundException.class)
       public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex);
       
       @ExceptionHandler(MethodArgumentNotValidException.class)
       public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex);
       
       @ExceptionHandler(Exception.class)
       public ResponseEntity<ErrorResponse> handleGeneral(Exception ex);
   }
   ```

4. **HTTP Status Code mapping**:
   | Exception | HTTP Status |
   |-----------|-------------|
   | `OrderNotFoundException` | 404 |
   | `MemberNotFoundException` | 404 |
   | `ProductNotFoundException` | 404 |
   | `MemberInactiveException` | 400 |
   | `InsufficientStockException` | 400 |
   | `ProductUnavailableException` | 400 |
   | `InvalidOrderStatusException` | 400 |
   | `PaymentFailedException` | 422 |
   | `MethodArgumentNotValidException` | 400 |
   | `Exception` (catch-all) | 500 |

### ✅ Tiêu chí DONE

- [ ] Tất cả error responses có format thống nhất
- [ ] Validation errors trả về field-level details
- [ ] HTTP status codes đúng theo mapping
- [ ] Log ERROR cho 5xx, WARN cho 4xx
- [ ] Không leak stack trace trong response

**Test commands**:
```bash
# Validation error (empty memberId)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"memberId": "", "items": [], "paymentMethod": "CREDIT_CARD"}'

# Should return 400 with fieldErrors
```

---

## Phase 5: Unit Testing

### 🎯 Mục tiêu
Viết unit tests cho business logic layer (Service).

### 📦 Output mong muốn

```
src/test/java/com/sotatek/order/
├── service/
│   └── OrderServiceTest.java
└── controller/
    └── OrderControllerTest.java (optional)
```

### 📝 Tasks chi tiết

1. **Create `OrderServiceTest.java`**:

   **Test cases cho `createOrder`**:
   - ✅ Happy path: valid request → order created với status CONFIRMED
   - ❌ Member not found → throw MemberNotFoundException
   - ❌ Member inactive → throw MemberInactiveException
   - ❌ Product not found → throw ProductNotFoundException
   - ❌ Insufficient stock → throw InsufficientStockException
   - ❌ Payment failed → order stays PENDING

   **Test cases cho `getOrder`**:
   - ✅ Order exists → return OrderResponse
   - ❌ Order not found → throw OrderNotFoundException

   **Test cases cho `cancelOrder`**:
   - ✅ PENDING → CANCELLED: success
   - ✅ CONFIRMED → CANCELLED: success
   - ❌ Already CANCELLED → throw InvalidOrderStatusException

2. **Setup test với Mockito**:
   ```java
   @ExtendWith(MockitoExtension.class)
   class OrderServiceTest {
       
       @Mock
       private OrderRepository orderRepository;
       
       @Mock
       private MemberClient memberClient;
       
       @Mock
       private ProductClient productClient;
       
       @Mock
       private PaymentClient paymentClient;
       
       @InjectMocks
       private OrderServiceImpl orderService;
       
       // Tests...
   }
   ```

3. **(Optional) Create `OrderControllerTest.java`** với `@WebMvcTest`

### ✅ Tiêu chí DONE

- [ ] `./gradlew test` pass tất cả tests
- [ ] Coverage cho happy path + major error cases
- [ ] Tối thiểu 8-10 test cases
- [ ] Không có flaky tests

---

## Phase 6: Documentation & Polish

### 🎯 Mục tiêu
Hoàn thiện documentation và cleanup code.

### 📦 Output mong muốn

```
├── README.md (updated với run instructions)
└── src/main/java/com/sotatek/order/
    └── config/
        └── OpenApiConfig.java
```

### 📝 Tasks chi tiết

1. **Create `OpenApiConfig.java`**:
   ```java
   @Configuration
   public class OpenApiConfig {
       @Bean
       public OpenAPI customOpenAPI() {
           return new OpenAPI()
               .info(new Info()
                   .title("Order Service API")
                   .version("1.0.0")
                   .description("Order management microservice"));
       }
   }
   ```

2. **Add Swagger annotations** to Controller (optional):
   - `@Operation(summary = "...")`
   - `@ApiResponse`

3. **Update README.md** với:
   - How to build: `./gradlew build`
   - How to run: `./gradlew bootRun`
   - API endpoints list
   - Design decisions summary

4. **Code cleanup**:
   - Remove unused imports
   - Add JavaDoc cho public methods
   - Consistent formatting

### ✅ Tiêu chí DONE

- [ ] Swagger UI accessible tại `http://localhost:8080/swagger-ui.html`
- [ ] README có đầy đủ instructions
- [ ] `./gradlew build` không có warnings
- [ ] Code formatted consistently

---

## 🚀 Final Checklist

Trước khi submit, verify tất cả items:

```
CORE FUNCTIONALITY:
[x] ./gradlew build passes
[x] ./gradlew test passes (all tests green)
[x] Application starts without error
[x] POST /api/orders works
[x] GET /api/orders/{id} works  
[x] GET /api/orders works (pagination)
[x] PUT /api/orders/{id} (cancel) works

EXTERNAL INTEGRATION:
[x] Member validation works
[x] Product validation works
[x] Payment processing works
[x] Error scenarios handled

CODE QUALITY:
[x] Consistent error response format
[x] Proper HTTP status codes
[x] Logging present
[x] No hardcoded values
[x] Clean package structure

DOCUMENTATION:
[x] Swagger UI works
[x] README has run instructions
```

---

## 📌 Notes for AI Agent

1. **Execute phases sequentially** - Each phase builds on the previous
2. **Verify DONE criteria** before moving to next phase
3. **Run `./gradlew build`** after each phase to catch errors early
4. **Use provided test commands** to verify functionality
5. **If stuck on a phase > 15 minutes**, simplify and move on
6. **Prioritize working code** over perfect code

---

## Phase 7: Critical Technical Debt (P0)

### 🎯 Mục tiêu
Giải quyết các vấn đề nghiêm trọng từ Technical Debt cần thiết cho production.

### 📝 Tasks chi tiết

#### 7.1. Race Condition - Stock Check
- **Issue**: Không có locking giữa check stock và create order → overselling
- **Solution**:
  - Thêm `@Version` field vào `Order` entity cho Optimistic Locking
  - Hoặc sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` trong repository

```java
// OrderRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdWithLock(@Param("id") Long id);
```

#### 7.2. Distributed Transaction - Saga Pattern
- **Issue**: Payment fail/timeout sau khi order đã saved → inconsistent state
- **Solution**:
  - Implement compensation logic khi payment fails
  - Add `PAYMENT_FAILED` status để track
  - Consider idempotency key cho retry safety

```java
// OrderStatus.java - thêm status mới
PAYMENT_FAILED

// OrderServiceImpl.java - compensation logic
try {
    processPayment(savedOrder);
} catch (PaymentFailedException e) {
    savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
    orderRepository.save(savedOrder);
    throw e;
}
```

#### 7.3. Database Migration với Flyway
- **Issue**: `ddl-auto: create-drop` → mất data khi restart
- **Solution**:
  - Add Flyway dependency
  - Create migration scripts
  - Switch `ddl-auto` sang `validate`

```groovy
// build.gradle
implementation 'org.flywaydb:flyway-core'

// application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

### ✅ Tiêu chí DONE
- [ ] Optimistic/Pessimistic locking implemented
- [ ] Payment failure có compensation logic
- [ ] Flyway migrations ready
- [ ] Tests vẫn pass

---

## Phase 8: Production Hardening (P1)

### 🎯 Mục tiêu
Tăng cường resilience và production-readiness.

### 📝 Tasks chi tiết

#### 8.1. Circuit Breaker với Resilience4j
- **Issue**: External service down → toàn bộ order service down
- **Solution**: Add Resilience4j với Circuit Breaker, Retry, và TimeLimiter

```groovy
// build.gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
```

```java
// MemberClient interface
@CircuitBreaker(name = "memberService", fallbackMethod = "getMemberFallback")
@Retry(name = "memberService")
@TimeLimiter(name = "memberService")
MemberResponse getMember(String memberId);

default MemberResponse getMemberFallback(String memberId, Throwable t) {
    throw new ServiceUnavailableException("Member service is temporarily unavailable");
}
```

#### 8.2. Fix Entity Lombok Issue
- **Issue**: `@Data` trên Entity gây N+1 queries, StackOverflowError
- **Solution**: Replace `@Data` với `@Getter`, `@Setter` và custom equals/hashCode

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {
    // ...
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return id != null && id.equals(order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

#### 8.3. Idempotency Key
- **Issue**: Retry request có thể tạo duplicate orders
- **Solution**: Add `Idempotency-Key` header support

```java
// IdempotencyKeyService.java
@Service
public class IdempotencyKeyService {
    private final Map<String, OrderResponse> cache = new ConcurrentHashMap<>();
    
    public Optional<OrderResponse> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }
    
    public void store(String key, OrderResponse response) {
        cache.put(key, response);
    }
}

// OrderController.java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody CreateOrderRequest request) {
    
    if (idempotencyKey != null) {
        Optional<OrderResponse> cached = idempotencyKeyService.get(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }
    }
    // ... continue with order creation
}
```

#### 8.4. Cancel Order với Refund
- **Issue**: Cancel CONFIRMED order không trigger refund
- **Solution**: Add refund logic trong cancel flow

```java
// PaymentClient.java
PaymentResponse refundPayment(String transactionId, BigDecimal amount);

// OrderServiceImpl.java - cancelOrder method
if (order.getStatus() == OrderStatus.CONFIRMED && order.getPaymentTransactionId() != null) {
    paymentClient.refundPayment(order.getPaymentTransactionId(), order.getTotalAmount());
}
```

### ✅ Tiêu chí DONE
- [ ] Circuit Breaker cho tất cả external calls
- [ ] Entity Lombok issues fixed
- [ ] Idempotency key working
- [ ] Refund on cancel implemented
- [ ] All tests pass

---

## Phase 9: Observability & Security (P2 - Nice to have)

### 📝 Tasks chi tiết

#### 9.1. Pagination với Sort
```java
@GetMapping
public ResponseEntity<Page<OrderResponse>> listOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "desc") String sortDir) {
    
    Sort sort = sortDir.equalsIgnoreCase("asc") 
        ? Sort.by(sortBy).ascending() 
        : Sort.by(sortBy).descending();
    return ResponseEntity.ok(orderService.listOrders(PageRequest.of(page, size, sort)));
}
```

#### 9.2. Error Response với Trace ID
```java
@Data
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String traceId;  // Add MDC.get("traceId")
    private Map<String, String> fieldErrors;
}
```

#### 9.3. Observability Stack
- [ ] Add Spring Boot Actuator
- [ ] Add Micrometer metrics
- [ ] Structured logging với correlation ID

#### 9.4. Security (Optional)
- [ ] Spring Security + JWT
- [ ] Rate limiting với Bucket4j
- [ ] Input sanitization

### ✅ Tiêu chí DONE
- [x] Sort parameter working
- [x] Trace ID in error responses
- [x] Actuator endpoints accessible
- [ ] (Optional) Basic security configured

---

## Phase 10: Final Polish & Bonus Points (P0 - Required for submission)

### 🎯 Mục tiêu
Hoàn thiện các items còn thiếu để đạt điểm tối đa theo README requirements.

### 📊 Priority Matrix

| Priority | Task | Impact | Effort |
|----------|------|--------|--------|
| 🔴 P0 | Docker Support | +++ Bonus Point | 15 phút |
| 🔴 P0 | Integration Tests | ++ Testing Score | 30 phút |
| 🟡 P1 | Update README (Design Decisions) | + Documentation | 15 phút |
| 🟢 P2 | Additional Unit Tests | + Coverage | 20 phút |

---

### 10.1. Docker Support (🔴 BONUS POINT - Required)

**Issue**: README yêu cầu Docker support như bonus point
**Impact**: +++ (Bonus điểm trực tiếp)

#### Files cần tạo:

**`Dockerfile`**:
```dockerfile
# Multi-stage build for smaller image
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY src src
RUN chmod +x gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Add non-root user for security
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
USER appuser

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`docker-compose.yml`**:
```yaml
version: '3.8'

services:
  order-service:
    build: .
    container_name: order-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xmx512m -Xms256m
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    restart: unless-stopped

  # Optional: PostgreSQL for production-like setup
  # postgres:
  #   image: postgres:15-alpine
  #   environment:
  #     POSTGRES_DB: orderdb
  #     POSTGRES_USER: order
  #     POSTGRES_PASSWORD: secret
  #   ports:
  #     - "5432:5432"
  #   volumes:
  #     - postgres_data:/var/lib/postgresql/data

# volumes:
#   postgres_data:
```

**`.dockerignore`**:
```
.git
.gitignore
.gradle
build
*.md
docs
.idea
*.iml
```

#### Verify commands:
```bash
# Build image
docker build -t order-service .

# Run container
docker-compose up -d

# Check health
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f
```

### ✅ Tiêu chí DONE - Docker
- [ ] `docker build` thành công
- [ ] `docker-compose up` chạy được
- [ ] Health check pass
- [ ] Container restart tự động

---

### 10.2. Integration Tests (🔴 HIGH PRIORITY)

**Issue**: README nói "Integration tests (optional but appreciated)"
**Impact**: ++ Testing score

#### File: `src/test/java/com/sotatek/order/controller/OrderControllerIntegrationTest.java`

```java
package com.sotatek.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.OrderItemRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.enums.OrderStatus;
import com.sotatek.order.model.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_Success_Returns201() throws Exception {
        CreateOrderRequest request = createValidRequest();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.memberId").value("M001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void createOrder_InvalidMember_Returns404() throws Exception {
        CreateOrderRequest request = createValidRequest();
        request.setMemberId("not-found");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void createOrder_InactiveMember_Returns400() throws Exception {
        CreateOrderRequest request = createValidRequest();
        request.setMemberId("inactive-member");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MEMBER_INACTIVE"));
    }

    @Test
    void createOrder_ValidationError_Returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setMemberId(""); // Invalid: blank
        request.setItems(List.of());
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void createOrder_IdempotencyKey_ReturnsSameResponse() throws Exception {
        CreateOrderRequest request = createValidRequest();
        String idempotencyKey = UUID.randomUUID().toString();

        // First request
        MvcResult first = mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request with same key
        MvcResult second = mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Should return same response
        assertEquals(first.getResponse().getContentAsString(),
                     second.getResponse().getContentAsString());
    }

    @Test
    void getOrder_Exists_Returns200() throws Exception {
        // First create an order
        CreateOrderRequest request = createValidRequest();
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract ID from response
        String response = createResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("id").asLong();

        // Get order
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.memberId").value("M001"));
    }

    @Test
    void getOrder_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void listOrders_WithPagination_Returns200() throws Exception {
        mockMvc.perform(get("/api/orders")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void cancelOrder_Success_Returns200() throws Exception {
        // First create an order
        CreateOrderRequest createReq = createValidRequest();
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(
            createResult.getResponse().getContentAsString()).get("id").asLong();

        // Cancel order
        UpdateOrderRequest cancelReq = new UpdateOrderRequest();
        cancelReq.setStatus(OrderStatus.CANCELLED);

        mockMvc.perform(put("/api/orders/{id}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_AlreadyCancelled_Returns400() throws Exception {
        // Create and cancel an order first
        CreateOrderRequest createReq = createValidRequest();
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();

        Long orderId = objectMapper.readTree(
            createResult.getResponse().getContentAsString()).get("id").asLong();

        UpdateOrderRequest cancelReq = new UpdateOrderRequest();
        cancelReq.setStatus(OrderStatus.CANCELLED);

        // First cancel
        mockMvc.perform(put("/api/orders/{id}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq)));

        // Second cancel - should fail
        mockMvc.perform(put("/api/orders/{id}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void errorResponse_ContainsTraceId() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private CreateOrderRequest createValidRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("P001");
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setMemberId("M001");
        request.setItems(List.of(item));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        return request;
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + " but was: " + actual);
        }
    }
}
```

### ✅ Tiêu chí DONE - Integration Tests
- [ ] Tất cả integration tests pass
- [ ] Test coverage cho happy path + error cases
- [ ] Tối thiểu 10 integration test cases
- [ ] Tests chạy với `./gradlew test`

---

### 10.3. Update README - Design Decisions (🟡 MEDIUM)

**Issue**: README hiện tại là template gốc, chưa có design decisions
**Impact**: + Documentation score

#### Thêm vào cuối README.md:

```markdown
---

## Implementation Details

### Design Decisions

1. **Architecture**: Layered architecture với clear separation
   - Controller → Service → Repository
   - Client interfaces cho external services (dễ mock/swap)

2. **Database**: H2 in-memory với Flyway migrations
   - Production-ready migration scripts
   - Easy to switch to PostgreSQL

3. **External Services**: Mock implementations với deterministic behavior
   - `memberId = "not-found"` → MemberNotFoundException
   - `productId = "out-of-stock"` → InsufficientStockException
   - `amount > 10000` → Payment FAILED

4. **Resilience Patterns**:
   - Circuit Breaker (Resilience4j) cho external calls
   - Retry với exponential backoff
   - Fallback methods khi service unavailable

5. **Idempotency**:
   - `Idempotency-Key` header support
   - Caffeine cache với TTL 10 phút

6. **Concurrency**:
   - Optimistic locking (`@Version`) cho Order entity
   - Pessimistic locking cho cancel operation

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create new order |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders` | List orders (paginated) |
| PUT | `/api/orders/{id}` | Cancel order |

### Running the Application

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Start application
./gradlew bootRun

# With Docker
docker-compose up -d
```

### Available Endpoints

- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
```

### ✅ Tiêu chí DONE - README
- [ ] Design decisions documented
- [ ] API endpoints listed
- [ ] Run instructions clear
- [ ] Docker instructions included

---

### 10.4. Additional Unit Tests (🟢 LOW - Already done)

**Status**: ✅ COMPLETED (27 tests)

Test cases đã được bổ sung:
- `createOrder_MultipleItems_Success`
- `createOrder_StockReturnsNull`
- `createOrder_PaymentReturnsNull`
- `createOrder_VerifyTotalAmountCalculation`
- `createOrder_ProductOutOfStock_ZeroQuantity`
- `cancelOrder_OrderNotFound`
- `cancelOrder_InvalidStatus_NotCancelled`
- `cancelOrder_RefundFailed`
- `cancelOrder_PaymentFailedOrder`

---

## 🚀 Final Submission Checklist

```
CORE FUNCTIONALITY:
[x] ./gradlew build passes
[x] ./gradlew test passes (27 tests)
[x] Application starts without error
[x] All CRUD endpoints work

EXTERNAL INTEGRATION:
[x] Member validation
[x] Product validation
[x] Payment processing
[x] Refund on cancel

RESILIENCE:
[x] Circuit Breaker
[x] Retry with exponential backoff
[x] Fallback methods
[x] Idempotency key

CODE QUALITY:
[x] Error response format consistent
[x] HTTP status codes correct
[x] Logging with traceId
[x] Clean package structure

BONUS POINTS:
[x] Circuit Breaker pattern ✓
[x] Retry mechanism ✓
[x] Logging & monitoring ✓
[ ] Docker support ← TODO
[x] Database migrations ✓

DOCUMENTATION:
[x] Swagger UI works
[ ] README updated ← TODO

TESTING:
[x] Unit tests (27 tests)
[ ] Integration tests ← TODO
```

**Estimated Score: 87% → Target 95%**

