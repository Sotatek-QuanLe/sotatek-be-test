# Order Microservice Challenge

Welcome! This is a backend developer assessment designed to evaluate your skills in building microservices.

## The Challenge

Your mission is to build an **Order Service** - a microservice that handles order management while integrating with external services (Member, Product, and Payment services).

**Time Limit**: 4 hours

Don't worry - we're not looking for perfection. We want to see how you approach problems, structure your code, and handle real-world microservice scenarios.

---

## System Architecture

```
                                    ┌─────────────────┐
                                    │  Member Service │
                                    │    (External)   │
                                    └────────┬────────┘
                                             │
┌──────────┐      ┌─────────────────┐       │        ┌─────────────────┐
│  Client  │─────▶│  Order Service  │───────┼───────▶│ Product Service │
└──────────┘      │   (Your Task)   │       │        │    (External)   │
                  └────────┬────────┘       │        └─────────────────┘
                           │                │
                           │                │        ┌─────────────────┐
                           │                └───────▶│ Payment Service │
                           │                         │    (External)   │
                           ▼                         └─────────────────┘
                  ┌─────────────────┐
                  │    Database     │
                  │  (Your Choice)  │
                  └─────────────────┘
```

**Note**: The external services (Member, Product, Payment) are provided as OpenAPI specs only.
You'll need to **mock these services** in your implementation.

---

## Requirements

### Functional Requirements

Build REST APIs for Order management with the following operations:

| Operation | Endpoint | Description |
|-----------|----------|-------------|
| Create Order | `POST /api/orders` | Create a new order |
| Get Order | `GET /api/orders/{id}` | Retrieve order details |
| List Orders | `GET /api/orders` | List orders (with pagination) |
| Update Order | `PUT /api/orders/{id}` | Cancel Order |

### External Service Integration

When creating or processing an order, your service must:

1. **Validate Member** - Call Member Service to verify the member exists and is active
2. **Check Product** - Call Product Service to verify product availability and stock
3. **Process Payment** - Call Payment Service when the order is confirmed

### Non-Functional Requirements

- Proper error handling and meaningful error messages
- Input validation
- Logging for debugging and monitoring
- Unit tests and/or integration tests

---

## Tech Stack

### Required
- **Java**: 17 or higher
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle

### Your Choice
- Database (H2, PostgreSQL, MySQL, etc.)
- HTTP Client (RestTemplate, WebClient, Feign, etc.)
- Any additional libraries you find useful

---

## External Service Specs

The OpenAPI specifications for external services are located in:

```
docs/api-specs/
├── member-service.yaml    # Member validation API
├── product-service.yaml   # Product & inventory API
└── payment-service.yaml   # Payment processing API
```

**Important**: These services don't actually exist - you need to mock them in your tests and implementation. Consider how you would handle:
- Service unavailability
- Timeout scenarios
- Error responses

---

## What to Submit

Create your own repository and include:

1. **Source Code**
   - Well-structured, clean code
   - Clear package organization

2. **Tests**
   - Unit tests for business logic
   - Integration tests (optional but appreciated)

3. **Documentation**
   - API documentation (Swagger/OpenAPI recommended)
   - Brief README explaining your design decisions

4. **How to Run**
   - Clear instructions to build and run your service
   - Any setup steps required

---

## Evaluation Criteria

We'll be looking at:

| Criteria | What We Look For |
|----------|------------------|
| **Code Quality** | Clean code, readability, SOLID principles |
| **Architecture** | Layer separation, dependency management, design patterns |
| **MSA Integration** | External service handling, error handling, resilience |
| **Testing** | Test coverage, test quality, mocking strategies |
| **API Design** | RESTful conventions, proper HTTP status codes |

### Bonus Points

These are optional but will make your submission stand out:

- Circuit Breaker pattern for external service calls
- Retry mechanism with exponential backoff
- Comprehensive logging and monitoring hooks
- Docker support
- Database migration scripts

---

## Getting Started

This repository provides a minimal Spring Boot application to get you started:

```bash
# Clone this repository for reference
git clone <this-repo-url>

# Check that it builds
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

Now create your own repository and start building!

---

## Tips

- **Don't overthink it** - A working solution with clean code is better than an over-engineered incomplete one
- **Time management** - Prioritize core functionality first, then add enhancements
- **Show your thinking** - Comments and documentation help us understand your approach
- **Test what matters** - Focus on testing critical business logic

---

## Implementation Details

### Design Decisions

1. **Architecture**: Layered architecture with clear separation
    - **Controller**: REST API endpoints for Order management.
    - **Service**: Business logic implementation with validation.
    - **Repository**: Data access layer using Spring Data JPA.
    - **Client**: Interface-based external service clients for easy mocking and testing.

2. **Database**: H2 in-memory database with **Flyway** migrations.
    - Production-ready migration scripts in `src/main/resources/db/migration`.
    - Automated schema validation and updates.

3. **External Service Integration**: Deterministic mock implementations.
    - **Member Service**: Status-based validation (ACTIVE/INACTIVE).
    - **Product Service**: Inventory and availability checks.
    - **Payment Service**: Successful and failed payment simulations.

4. **Resilience & Fault Tolerance**:
    - **Circuit Breaker (Resilience4j)**: Protects order creation from external service failures.
    - **Retry Mechanism**: Exponential backoff for transient failures.
    - **Fallbacks**: Graceful error handling when services are unavailable.

5. **Idempotency**:
    - Support for `Idempotency-Key` header in `POST /api/orders`.
    - Caffeine-based caching with TTL to prevent duplicate orders.

6. **Concurrency & Locking**:
    - **Optimistic Locking**: `@Version` field to prevent lost updates on Order status.
    - **Pessimistic Locking**: `LockModeType.PESSIMISTIC_WRITE` for cancellation to ensure single-thread access.

7. **Error Handling**:
    - Centralized `GlobalExceptionHandler`.
    - Standardized `ErrorResponse` with `traceId` for observability.

### API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create a new order (with Idempotency support) |
| GET | `/api/orders/{id}` | Retrieve order details |
| GET | `/api/orders` | List orders (supports pagination & sorting) |
| PUT | `/api/orders/{id}` | Update (Cancel) an order |

### Running the Application

#### Using Gradle
```bash
# Build the project
./gradlew build

# Run unit and integration tests
./gradlew test

# Start the application
./gradlew bootRun
```

#### Using Docker
```bash
# Build and start container
docker-compose up -d --build

# Check health
curl http://localhost:8080/actuator/health
```

### Documentation
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Actuator Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Prometheus Metrics**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
