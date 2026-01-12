# Assumptions

Các giả định được đưa ra trong bối cảnh technical assignment 4 giờ.

---

## 1. Business Assumptions

### Order Flow
- Một order có thể chứa **nhiều products** (shopping cart style)
- Mỗi order có nhiều **OrderItem**, mỗi item có productId và quantity
- Order statuses (simplified):
  - `PENDING` - Order đã tạo, chưa thanh toán
  - `CONFIRMED` - Đã thanh toán thành công
  - `CANCELLED` - Order bị hủy bởi user
- Order flow: `PENDING` → `CONFIRMED` hoặc `CANCELLED`
- Cả `PENDING` và `CONFIRMED` đều có thể chuyển sang `CANCELLED`
- Không hỗ trợ partial payment hoặc split payment
- Không có khái niệm shipping/delivery trong scope này

### Order Update Rules
- **PUT /api/orders/{id}**: Chỉ cho phép update **status** (cancel order)
- Không cho phép update các field khác (memberId, items)
- Valid transitions: `PENDING` → `CANCELLED`, `CONFIRMED` → `CANCELLED`
- Đã `CANCELLED` thì không thể thay đổi nữa
- ~~DELETE /api/orders/{id}~~: **Removed from scope**

### Member
- Member đã được authenticate ở layer khác (API Gateway/Auth Service)
- `memberId` được truyền vào request, không cần verify JWT/token
- Chỉ validate member **exists** và **status = ACTIVE**

### Product & Inventory
- Stock check là **point-in-time** validation, không lock inventory
- Không handle race condition khi nhiều orders cùng lúc (giả định low traffic)
- Price được lấy từ Product Service tại thời điểm tạo order (snapshot pricing)

### Payment
- Payment là **synchronous** - response ngay lập tức (COMPLETED hoặc FAILED)
- Không handle async payment callbacks/webhooks
- Không implement refund flow

---

## 2. Technical Assumptions

### Database
- Sử dụng **H2 in-memory database** cho development/demo
- Schema được tạo tự động bởi Hibernate (`ddl-auto: create-drop`)
- Không cần database migration scripts (Flyway/Liquibase)

### External Services
- External services (Member, Product, Payment) được **mock trong code**
- Mock trả về deterministic response dựa trên input
- Không setup WireMock server riêng biệt

### API Design
- Tất cả APIs đều synchronous (blocking)
- Pagination sử dụng offset-based (`page`, `size` params)
- Response format thống nhất cho cả success và error

### Security
- **Không implement authentication/authorization** trong scope này
- Giả định request đã được xác thực ở API Gateway layer
- Không validate/sanitize input cho SQL injection (JPA handles this)

---

## 3. Data Assumptions

### IDs
- Sử dụng **auto-generated Long ID** cho Order entity
- External IDs (memberId, productId) được truyền vào dưới dạng String

### Validation
- `memberId` - required, non-empty
- `items` - required, non-empty array (at least 1 item)
  - `items[].productId` - required, non-empty
  - `items[].quantity` - required, positive integer (> 0)
- `paymentMethod` - required, enum: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER

### Timestamps
- Sử dụng `LocalDateTime` với timezone UTC
- `createdAt` và `updatedAt` được tự động quản lý

---

## 4. Out of Scope

Những feature **không implement** trong assignment này:

- [ ] Multi-item orders (cart với nhiều products)
- [ ] Inventory reservation/locking
- [ ] Async payment processing
- [ ] Order history/audit log
- [ ] Email/notification service
- [ ] Rate limiting
- [ ] Caching (Redis)
- [ ] Distributed tracing
- [ ] Health check endpoints (beyond basic actuator)
- [ ] Containerization (Docker)

---

## 5. Mock Service Behavior

### Member Service Mock
| Input | Response |
|-------|----------|
| `memberId` exists | `200 OK` - member details |
| `memberId` = "inactive-member" | `200 OK` - status: INACTIVE |
| `memberId` = "not-found" | `404 Not Found` |
| `memberId` = "error" | `500 Internal Server Error` |

### Product Service Mock
| Input | Response |
|-------|----------|
| `productId` exists | `200 OK` - product details |
| `productId` = "out-of-stock" | `200 OK` - stock: 0 |
| `productId` = "discontinued" | `200 OK` - status: DISCONTINUED |
| `productId` = "not-found" | `404 Not Found` |

### Payment Service Mock
| Input | Response |
|-------|----------|
| Valid payment request | `200 OK` - status: COMPLETED |
| `paymentMethod` = invalid | `400 Bad Request` |
| Amount > 10000 | `200 OK` - status: FAILED (simulate decline) |

---

## 6. Error Handling Strategy

- **4xx errors**: Client errors - validation failed, resource not found
- **5xx errors**: Server errors - external service unavailable, unexpected exceptions
- Tất cả errors trả về format thống nhất:
  ```json
  {
    "error": "ERROR_CODE",
    "message": "Human readable message",
    "timestamp": "2024-01-15T10:30:00Z"
  }
  ```

---

## 7. Assumptions Risk Assessment (Self-Critique)

> *"An assumption is a liability until proven otherwise."*

### 7.1 Risk Matrix

| Assumption | Category | Risk Level | Failure Mode |
|------------|----------|------------|--------------|
| Low traffic | Business | 🔴 CRITICAL | Race condition → overselling |
| Sync payment | Technical | 🔴 CRITICAL | Timeout → stuck orders |
| No refund flow | Business | 🟠 HIGH | Chargebacks, complaints |
| Mock = Real behavior | Technical | 🟠 HIGH | Production failures |
| H2 in-memory | Technical | 🟡 MEDIUM | Data loss on restart |
| No auth | Security | 🟡 MEDIUM | Fraud, unauthorized orders |
| Point-in-time price | Business | 🟡 MEDIUM | Price disputes |

### 7.2 Critical Assumptions Deep Dive

#### 🔴 "Low Traffic" - The Most Dangerous Assumption

```
Why we made it:
- Simplifies implementation
- Avoids distributed locking complexity
- Saves 30+ minutes of coding

Why it's dangerous:
- Traffic is unpredictable
- Marketing campaigns, viral moments
- Single viral tweet = 10,000x traffic spike

What breaks:
- Stock check race condition
- Duplicate order creation
- Payment double-charge

Production fix needed:
- Redis distributed lock on (productId + stock operation)
- Idempotency key for order creation
- Pessimistic DB locking for critical sections
```

#### 🔴 "Synchronous Payment" - Ticking Time Bomb

```
Why we made it:
- Simpler request/response flow
- No webhook infrastructure needed
- Easier to test and debug

Why it's dangerous:
- Real payment gateways timeout (30s+)
- Network issues cause hanging requests
- Thread pool exhaustion under load

What breaks:
- User sees "loading" forever
- Order status inconsistent
- Double charges on retry

Production fix needed:
- Async payment initiation
- Webhook for payment confirmation
- Polling endpoint for status check
- Timeout + retry with idempotency
```

#### 🟠 "No Refund on Cancel" - Customer Trust Killer

```
Why we made it:
- Out of scope per time constraint
- Simplifies cancel logic
- Avoids Payment Service complexity

Why it's dangerous:
- CONFIRMED order has money charged
- Cancel without refund = angry customer
- Chargeback = penalty fees + reputation damage

What breaks:
- Customer trust
- Legal compliance (depending on jurisdiction)
- Support ticket flood

Production fix needed:
- PaymentClient.refund(orderId, amount)
- Partial refund support
- Refund status tracking
- Notification to customer
```

### 7.3 Assumptions That Are Actually OK

| Assumption | Why It's Acceptable |
|------------|---------------------|
| Single order service instance | Demo scope, horizontal scaling is deployment concern |
| No caching | Premature optimization, add when needed |
| No distributed tracing | Nice-to-have, not blocking |
| Offset pagination | Works fine for <100k records |
| UTC timestamps | Industry standard, correct choice |
| Auto-generated IDs | Simple, works for most cases |

### 7.4 Questions Interviewer Might Ask

| Question | Expected Answer |
|----------|-----------------|
| "What if 2 users order the last item?" | "Race condition - I'd add Redis lock in production" |
| "What if payment takes 30 seconds?" | "Current design will timeout - async + webhook needed" |
| "What happens to refund on cancel?" | "Not implemented - would add PaymentClient.refund()" |
| "Why H2?" | "Demo purpose - production would use PostgreSQL/MySQL" |
| "How do you prevent duplicate orders?" | "Missing idempotency key - production must have it" |

> **Self-awareness > Perfect design**: Interviewer values candidates who understand their design's limitations.
