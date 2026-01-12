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
[ ] ./gradlew test passes (all tests green)
[ ] Application starts without error
[ ] POST /api/orders works
[ ] GET /api/orders/{id} works  
[ ] GET /api/orders works (pagination)
[ ] PUT /api/orders/{id} (cancel) works

EXTERNAL INTEGRATION:
[ ] Member validation works
[ ] Product validation works
[ ] Payment processing works
[ ] Error scenarios handled

CODE QUALITY:
[ ] Consistent error response format
[ ] Proper HTTP status codes
[ ] Logging present
[ ] No hardcoded values
[ ] Clean package structure

DOCUMENTATION:
[ ] Swagger UI works
[ ] README has run instructions
```

---

## 📌 Notes for AI Agent

1. **Execute phases sequentially** - Each phase builds on the previous
2. **Verify DONE criteria** before moving to next phase
3. **Run `./gradlew build`** after each phase to catch errors early
4. **Use provided test commands** to verify functionality
5. **If stuck on a phase > 15 minutes**, simplify and move on
6. **Prioritize working code** over perfect code
