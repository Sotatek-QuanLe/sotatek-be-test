# Coding Conventions

> Minimal conventions for the 4-hour assignment. Read in 1 minute, apply throughout.

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

**Why**: Allows reviewers to locate code within 10 seconds.

---

## 2. Naming

| Type | Pattern | Example |
|------|---------|---------|
| DTO in | `*Request` | `CreateOrderRequest` |
| DTO out | `*Response` | `OrderResponse` |
| Exception | `*Exception` | `OrderNotFoundException` |
| Interface | Noun | `OrderService` |
| Impl | `*Impl` | `OrderServiceImpl` |

**Why**: Predictable; clear understanding of class purpose without reading the code.

---

## 3. API Response Format

```json
// Success
{ "id": 1, "status": "CONFIRMED", ... }

// Error
{ "error": "ORDER_NOT_FOUND", "message": "Order 99 not found" }
```

**Why**: Single unified format, avoiding confusion.

---

## 4. HTTP Status Codes

| Case | Status |
|------|--------|
| Created | `201` |
| Success | `200` |
| Not found | `404` |
| Bad input | `400` |
| Server error | `500` |

**Why**: Standard RESTful practice, expected by reviewers.

---

## 5. Logging

Log only in these two places:

```java
// Service layer - entry point
log.info("Creating order for member: {}", memberId);

// Exception handler - errors
log.error("Order not found: {}", id);
```

**Why**: Sufficient for debugging without spamming the console.

---

---

## 6. Git Commit Messages

```
<type>: <description>
```

**Types (only 4 used):**

| Type | When to use |
|------|--------------|
| `feat` | Adding a new feature |
| `fix` | Bug fix |
| `refactor` | Code changes without behavior modification |
| `docs` | Documentation updates |

**Examples:**
```
feat: add Order entity and repository
feat: implement create order API
fix: handle null pointer in stock check
docs: add API documentation
```

**Why**: Clean history; reviewers can instantly track progress.

---

## Not Required (4-Hour Scope)

| Skip | Reason |
|------|-------|
| JavaDoc | Code should be self-explanatory |
| Comments | Only for complex logic |
| Custom validators | Use built-in `@NotNull`, `@Min` |
| Abstract base classes | Over-engineering |
| Separate constants file | Inline is sufficient |
| Branch naming | Solo project, 4 hours |
| Commit body/footer | Overkill |
