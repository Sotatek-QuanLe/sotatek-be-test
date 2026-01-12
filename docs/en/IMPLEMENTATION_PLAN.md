# Master Implementation Plan

> **Purpose**: Guides the AI Agent to implement each phase independently and verifiably.

---

## 📋 Overview

| Phase | Name | Duration | Main Goal |
|-------|------|-----------|----------------|
| 1 | Project Foundation | 30 mins | Setup project, entities, database |
| 2 | Core Order APIs | 60 mins | Complete CRUD endpoints |
| 3 | External Service Integration | 45 mins | Mock clients + validation flow |
| 4 | Error Handling & Validation | 30 mins | Global exception handler, input validation |
| 5 | Unit Testing | 30 mins | Service layer tests |
| 6 | Documentation & Polish | 15 mins | Swagger, README, cleanup |

**Total Estimated Duration**: ~3.5 hours (30 min buffer for debugging)

---

## Phase 1: Project Foundation

### 🎯 Goal
Setup the project foundation with necessary dependencies, entity models, and database configuration.

### 📦 Desired Output

```
src/main/java/com/sotatek/order/
├── OrderApplication.java (existing)
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
└── application.yml (updated with H2 config)
```

### 📝 Detailed Tasks

1. **Update `build.gradle`** - Add dependencies:
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
   - Relationship: `@OneToMany` with `OrderItem`
   - Annotations: `@Entity`, `@Data`, `@EntityListeners(AuditingEntityListener.class)`

5. **Create `OrderItem.java`** entity:
   - Fields: `id`, `productId`, `productName`, `quantity`, `unitPrice`, `subtotal`
   - Relationship: `@ManyToOne` with `Order`

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

### ✅ DONE Criteria

- [ ] `./gradlew build` passes without errors
- [ ] `./gradlew bootRun` starts successfully
- [ ] Access `http://localhost:8080/h2-console` and see `orders` and `order_items` tables
- [ ] No warnings about missing dependencies

---

## Phase 2: Core Order APIs

### 🎯 Goal
Implement full CRUD operations for Order (without external validation).

### 📦 Desired Output

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

### 📝 Detailed Tasks

1. **Create Request DTOs**:
   - `CreateOrderRequest.java`
   - `OrderItemRequest.java`
   - `UpdateOrderRequest.java`

2. **Create Response DTOs**:
   - `OrderResponse.java`
   - `OrderItemResponse.java`

3. **Create custom exceptions**:
   - `OrderNotFoundException extends RuntimeException`
   - `InvalidOrderStatusException extends RuntimeException`

4. **Create `OrderService` interface**

5. **Create `OrderServiceImpl`**:
   - Implement all methods
   - Temporarily hardcode product info (will replace with external call in Phase 3)
   - Business logic:
     - `createOrder`: Create order with status `PENDING`, calculate `totalAmount`
     - `getOrder`: Find by ID, throw `OrderNotFoundException` if not found
     - `listOrders`: Return `Page<OrderResponse>`
     - `cancelOrder`: Only allow cancel if status != `CANCELLED`

6. **Create `OrderController`**

### ✅ DONE Criteria

- [ ] `./gradlew build` passes
- [ ] POST `/api/orders` returns 201 with order data
- [ ] GET `/api/orders/{id}` returns the created order
- [ ] GET `/api/orders?page=0&size=10` returns paginated list
- [ ] PUT `/api/orders/{id}` with `{"status": "CANCELLED"}` works
- [ ] GET non-existent order returns 404

---

## Phase 3: External Service Integration

### 🎯 Goal
Implement mock clients for Member, Product, Payment services and integrate into order flow.

### 📦 Desired Output

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

### 📝 Detailed Tasks

1. **Create External DTOs**

2. **Create Client Interfaces**: `MemberClient`, `ProductClient`, `PaymentClient`

3. **Create Mock Implementations** (according to behavior table in ASSUMPTIONS.md)

4. **Create custom exceptions** for external services

5. **Update `OrderServiceImpl`**:
   - Inject `MemberClient`, `ProductClient`, `PaymentClient`
   - In `createOrder`:
     1. Validate member (exists + ACTIVE)
     2. Validate each product (exists + AVAILABLE + has stock)
     3. Calculate `totalAmount` from product prices
     4. Save order with status `PENDING`
     5. Process payment
     6. Update status to `CONFIRMED` if payment success

### ✅ DONE Criteria

- [ ] Create order with valid member → success
- [ ] Create order with `memberId = "not-found"` → 404 error
- [ ] Create order with `memberId = "inactive-member"` → 400 error
- [ ] Create order with `productId = "not-found"` → 404 error
- [ ] Create order with `productId = "out-of-stock"` → 400 error
- [ ] Create order with `totalAmount > 10000` → payment failed, order stays `PENDING`
- [ ] Logging shows validation steps

---

## Phase 4: Error Handling & Validation

### 🎯 Goal
Implement global exception handler and standardize error response format.

### 📦 Desired Output

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

### 📝 Detailed Tasks

1. **Create `ErrorCode.java`** enum

2. **Create `ErrorResponse.java`**

3. **Create `GlobalExceptionHandler.java`**

4. **HTTP Status Code mapping**

### ✅ DONE Criteria

- [ ] All error responses have unified format
- [ ] Validation errors return field-level details
- [ ] HTTP status codes align with mapping
- [ ] ERROR log for 5xx, WARN log for 4xx
- [ ] No stack trace leaked in response

---

## Phase 5: Unit Testing

### 🎯 Goal
Write unit tests for business logic layer (Service).

### 📦 Desired Output

```
src/test/java/com/sotatek/order/
├── service/
│   └── OrderServiceTest.java
└── controller/
    └── OrderControllerTest.java (optional)
```

### 📝 Detailed Tasks

1. **Create `OrderServiceTest.java`**:
   - Test cases for `createOrder` (Happy path & Error cases)
   - Test cases for `getOrder`
   - Test cases for `cancelOrder`

2. **Setup test with Mockito**

3. **(Optional) Create `OrderControllerTest.java`** with `@WebMvcTest`

### ✅ DONE Criteria

- [ ] `./gradlew test` passes all tests
- [ ] Coverage for happy path + major error cases
- [ ] Minimum 8-10 test cases
- [ ] No flaky tests

---

## Phase 6: Documentation & Polish

### 🎯 Goal
Finalize documentation and code cleanup.

### 📦 Desired Output

```
├── README.md (updated with run instructions)
└── src/main/java/com/sotatek/order/
    └── config/
        └── OpenApiConfig.java
```

### 📝 Detailed Tasks

1. **Create `OpenApiConfig.java`**

2. **Add Swagger annotations** to Controller (optional)

3. **Update README.md** with:
   - Build/Run instructions
   - API endpoints list
   - Design decisions summary

4. **Code cleanup**:
   - Remove unused imports
   - Add JavaDoc for public methods
   - Consistent formatting

### ✅ DONE Criteria

- [ ] Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- [ ] README has complete instructions
- [ ] `./gradlew build` has no warnings
- [ ] Code formatted consistently

---

## 🚀 Final Checklist

Verify all items before submission:

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

### 🎯 Goal
Resolve critical technical debt issues required for production.

### 📝 Detailed Tasks

#### 7.1. Race Condition - Stock Check
- **Issue**: No locking between check stock and create order → overselling
- **Solution**:
  - Add `@Version` field to `Order` entity for Optimistic Locking
  - Or use `@Lock(LockModeType.PESSIMISTIC_WRITE)` in repository

#### 7.2. Distributed Transaction - Saga Pattern
- **Issue**: Payment fail/timeout after order saved → inconsistent state
- **Solution**:
  - Implement compensation logic when payment fails
  - Add `PAYMENT_FAILED` status to track
  - Consider idempotency key for retry safety

#### 7.3. Database Migration with Flyway
- **Issue**: `ddl-auto: create-drop` → data loss on restart
- **Solution**:
  - Add Flyway dependency
  - Create migration scripts
  - Switch `ddl-auto` to `validate`

### ✅ DONE Criteria
- [ ] Optimistic/Pessimistic locking implemented
- [ ] Payment failure implies compensation logic
- [ ] Flyway migrations ready
- [ ] Tests still pass

---

## Phase 8: Production Hardening (P1)

### 🎯 Goal
Increase resilience and production-readiness.

### 📝 Detailed Tasks

#### 8.1. Circuit Breaker with Resilience4j
- **Issue**: External service down → entire order service down
- **Solution**: Add Resilience4j with Circuit Breaker, Retry, and TimeLimiter

#### 8.2. Fix Entity Lombok Issue
- **Issue**: `@Data` on Entity causes N+1 queries, StackOverflowError
- **Solution**: Replace `@Data` with `@Getter`, `@Setter` and custom equals/hashCode

#### 8.3. Idempotency Key
- **Issue**: Retry request can create duplicate orders
- **Solution**: Add `Idempotency-Key` header support

#### 8.4. Cancel Order with Refund
- **Issue**: Cancel CONFIRMED order does not trigger refund
- **Solution**: Add refund logic in cancel flow

### ✅ DONE Criteria
- [ ] Circuit Breaker for all external calls
- [ ] Entity Lombok issues fixed
- [ ] Idempotency key working
- [ ] Refund on cancel implemented
- [ ] All tests pass

---

## Phase 9: Observability & Security (P2 - Nice to have)

### 📝 Detailed Tasks

#### 9.1. Pagination with Sort
#### 9.2. Error Response with Trace ID
#### 9.3. Observability Stack
- [ ] Add Spring Boot Actuator
- [ ] Add Micrometer metrics
- [ ] Structured logging with correlation ID

#### 9.4. Security (Optional)
- [ ] Spring Security + JWT
- [ ] Rate limiting with Bucket4j
- [ ] Input sanitization

### ✅ DONE Criteria
- [x] Sort parameter working
- [x] Trace ID in error responses
- [x] Actuator endpoints accessible
- [ ] (Optional) Basic security configured

---

## Phase 10: Final Polish & Bonus Points (P0 - Required for submission)

### 🎯 Goal
Complete missing items to achieve maximum score according to README requirements.

### 📊 Priority Matrix

| Priority | Task | Impact | Effort |
|----------|------|--------|--------|
| 🔴 P0 | Docker Support | +++ Bonus Point | 15 mins |
| 🔴 P0 | Integration Tests | ++ Testing Score | 30 mins |
| 🟡 P1 | Update README (Design Decisions) | + Documentation | 15 mins |
| 🟢 P2 | Additional Unit Tests | + Coverage | 20 mins |

### 10.1. Docker Support (🔴 BONUS POINT - Required)

**Issue**: README requests Docker support as a bonus point
**Impact**: +++ (Direct Bonus Point)

### 10.2. Integration Tests (🔴 HIGH PRIORITY)

**Issue**: README says "Integration tests (optional but appreciated)"
**Impact**: ++ Testing score

### 10.3. Update README - Design Decisions (🟡 MEDIUM)

**Issue**: Current README is a template, lacking design decisions
**Impact**: + Documentation score

### 10.4. Additional Unit Tests (🟢 LOW - Already done)

**Status**: ✅ COMPLETED (27 tests)

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
