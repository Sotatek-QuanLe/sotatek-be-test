# Order Microservice - Coding Challenge Implementation

This is a robust implementation of the Order Microservice challenge, built with Spring Boot 3.2.0 and Java 17.

## 📖 Tài liệu Dự án
- [Tài liệu Thiết kế](DESIGN.md)
- [Các Giả định](ASSUMPTIONS.md)
- [Kế hoạch Triển khai](IMPLEMENTATION_PLAN.md)
- [Nợ Kỹ thuật](TECHNICAL_DEBT.md)
- [Quy chuẩn Coding](CONVENTIONS.md)

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Gradle 8.x (wrapper included)

### Build & Run
```bash
# Clone the repository
git clone <this-repo-url>

# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

### API Documentation (Swagger UI)
Once the application is running, you can access the interactive API documentation at:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🏗️ Design Decisions

### Clean Architecture & Layer Separation
- **Controller Layer**: Handles REST endpoints, input validation, and maps requests to service models.
- **Service Layer**: Contains core business logic, orchestrates calls to external service clients, and manages transactions.
- **Client Layer**: Encapsulates external service interactions using mock implementations (Phase 3 requirement).
- **Model/Entity Layer**: Separation between JPA Entities (`Order`, `OrderItem`) and DTOs for API/External requests.

### Key Features & Robustness
- **Global Error Handling**: Centralized exception management via `@RestControllerAdvice` ensuring consistent JSON error responses with `ErrorCode` and `timestamp`.
- **Payment Integrity**: Implements logic to log and store `paymentTransactionId` from the payment service before confirming the order.
- **Defensive Coding**: Strict null checks for external service responses and robust validation for input (e.g., max items, quantity limits).
- **Precision Accounting**: Uses `BigDecimal` with `RoundingMode.HALF_UP` for all currency calculations to avoid floating-point inaccuracies.
- **Lombok**: Extensive use of Lombok to reduce boilerplate code.

---

## 🛠️ Tech Stack
- **Framework**: Spring Boot 3.2.0
- **Database**: H2 (In-memory) for simplified evaluation
- **Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Validation**: Jakarta Bean Validation
- **Logging**: SLF4J + Logback

---

## 🧪 Testing
The project includes comprehensive unit tests covering happy paths and edge cases (e.g., member inactive, insufficient stock, payment failures).

```bash
# Run tests
./gradlew test
```

Currently, **14 test cases** are implemented and passing.

---

## 📌 API Endpoints Summary

| Operation | Method | Endpoint | Description |
|-----------|--------|----------|-------------|
| Create Order | `POST` | `/api/orders` | Validates data and processes payment |
| Get Order | `GET`  | `/api/orders/{id}` | Retrieves order details |
| List Orders | `GET`  | `/api/orders` | Paginated list of all orders |
| Cancel Order | `PUT`  | `/api/orders/{id}` | Cancels an existing order |

---

Developed as a part of the Backend Developer Assessment.
