# Coding Conventions

> Minimal conventions cho 4h assignment. Đọc 1 phút, apply xuyên suốt.

---

## 1. Package Structure

```
com.sotatek.order/
├── controller/     # REST endpoints
├── service/impl/   # Business logic
├── repository/     # Data access
├── model/
│   ├── entity/     # JPA entities
│   └── dto/        # Request/Response
├── client/impl/    # External service calls
└── exception/      # Custom exceptions + handler
```

**Why**: Reviewer tìm code trong 10 giây.

---

## 2. Naming

| Type | Pattern | Example |
|------|---------|---------|
| DTO in | `*Request` | `CreateOrderRequest` |
| DTO out | `*Response` | `OrderResponse` |
| Exception | `*Exception` | `OrderNotFoundException` |
| Interface | Noun | `OrderService` |
| Impl | `*Impl` | `OrderServiceImpl` |

**Why**: Predictable, không cần đọc code để biết class làm gì.

---

## 3. API Response Format

```json
// Success
{ "id": 1, "status": "CONFIRMED", ... }

// Error
{ "error": "ORDER_NOT_FOUND", "message": "Order 99 not found" }
```

**Why**: Một format duy nhất, không lẫn lộn.

---

## 4. HTTP Status Codes

| Case | Status |
|------|--------|
| Created | `201` |
| Success | `200` |
| Not found | `404` |
| Bad input | `400` |
| Server error | `500` |

**Why**: RESTful chuẩn, reviewer expect điều này.

---

## 5. Logging

Chỉ log ở 2 chỗ:

```java
// Service layer - entry point
log.info("Creating order for member: {}", memberId);

// Exception handler - errors
log.error("Order not found: {}", id);
```

**Why**: Đủ để debug, không spam console.

---

---

## 6. Git Commit Messages

```
<type>: <description>
```

**Types (chỉ 4 cái):**

| Type | Khi nào dùng |
|------|--------------|
| `feat` | Thêm feature mới |
| `fix` | Sửa bug |
| `refactor` | Thay đổi code, không đổi behavior |
| `docs` | Documentation |

**Ví dụ:**
```
feat: add Order entity and repository
feat: implement create order API
fix: handle null pointer in stock check
docs: add API documentation
```

**Why**: History sạch, reviewer lướt qua biết ngay progress.

---

## Không cần trong 4h

| Skip | Lý do |
|------|-------|
| JavaDoc | Code tự giải thích |
| Comments | Trừ logic phức tạp |
| Custom validators | Dùng built-in `@NotNull`, `@Min` |
| Abstract base classes | Over-engineering |
| Constants file riêng | Inline đủ rồi |
| Branch naming | Solo project, 4h |
| Commit body/footer | Overkill |
