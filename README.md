# ModernBank — Banking Backend API

A RESTful banking backend application built using **Java 21, Spring Boot, Spring Security, PostgreSQL, and Flyway**.

ModernBank demonstrates backend engineering concepts such as JWT authentication, account management, financial transactions, concurrency control, database transactions, and automated integration testing.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming |
| Spring Boot | REST API development |
| Spring Security | Authentication and authorization |
| JWT | Token-based authentication |
| Spring Data JPA / Hibernate | Database persistence |
| PostgreSQL | Relational database |
| Flyway | Database schema migrations |
| Maven | Build and dependency management |
| JUnit 5 / MockMvc | Automated integration testing |
| Git & GitHub | Version control |

## Features

### Authentication and User Management

- Customer registration
- Secure password hashing
- JWT-based login
- Protected API endpoints
- Authenticated user profile

### Bank Account Management

- Create bank accounts
- View customer-owned accounts
- Account ownership validation
- Account status and currency handling

### Banking Transactions

- Deposit money
- Withdraw money
- Check available account balance
- View transaction history
- Prevent withdrawals with insufficient funds

### Account-to-Account Transfers

- Transfer money between bank accounts
- Validate sender ownership
- Prevent transfers to the same account
- Reject transfers with insufficient funds
- Record outgoing and incoming transactions
- Prevent duplicate transfers using idempotency keys
- Use database locking to protect balances during concurrent transfers

## Architecture

The application follows a layered backend architecture:

```text
Client
   |
   v
REST Controller
   |
   v
Service Layer
   |
   v
Repository Layer
   |
   v
PostgreSQL Database
```

**Controller layer:** Handles HTTP requests, responses, and API endpoints.

**Service layer:** Implements business rules and banking operations.

**Repository layer:** Uses Spring Data JPA for database access.

**Database layer:** Stores users, accounts, transactions, and transfers.

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a customer |
| POST | `/api/v1/auth/login` | Authenticate a customer |
| GET | `/api/v1/users/me` | View authenticated user |
| POST | `/api/v1/transfers` | Transfer money between accounts |

Additional account and transaction endpoints support account creation, deposits, withdrawals, and transaction history.

Protected endpoints require a JWT access token.

## Money Transfer Reliability

Financial transactions require special attention to data consistency.

ModernBank implements:

**Database transactions:** Spring's `@Transactional` ensures that related database changes commit together or roll back on failure.

**Pessimistic locking:** Account rows are locked during transfers to protect against concurrent balance modifications.

**Consistent lock ordering:** Account locks are acquired in ascending account ID order to reduce deadlock risk.

**Idempotency:** Repeated requests using the same idempotency key and matching transfer details return the existing transfer rather than moving money again.

**Transaction history:** Successful transfers generate linked `TRANSFER_OUT` and `TRANSFER_IN` records.

## Database Management

PostgreSQL is used for persistent storage.

Flyway manages database schema changes through versioned SQL migrations in:

`src/main/resources/db/migration/`

Hibernate schema validation is enabled to detect mismatches between JPA entities and the database schema.

## Running Locally

### Prerequisites

- Java 21
- PostgreSQL 16 or a compatible version
- Git
- An IDE such as VS Code or IntelliJ IDEA

### 1. Clone the repository

```bash
git clone https://github.com/Spoortikavatage/modernbank.git
cd modernbank
```

### 2. Configure PostgreSQL

Create a PostgreSQL database named:

```text
modernbank_db
```

Create a database user with the required privileges for connecting to the database and applying Flyway migrations.

The default application configuration uses:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/modernbank_db
spring.datasource.username=modernbank_app
spring.datasource.password=${MODERNBANK_DB_PASSWORD}
```

### 3. Configure environment variables

Set the following environment variables in your local environment:

```text
MODERNBANK_DB_PASSWORD
MODERNBANK_JWT_SECRET
```

Use a strong JWT signing secret and do not commit real credentials to GitHub.

### 4. Start the application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The application runs locally on:

`http://localhost:8081`

Health endpoint:

`http://localhost:8081/actuator/health`

## Testing

The project includes automated integration tests for:

- Authentication
- Account management
- Banking transactions
- Successful transfers
- Duplicate-transfer prevention
- Insufficient-balance handling
- Account ownership protection
- Same-account transfer rejection
- Concurrent-transfer safety

The integration tests use a separate PostgreSQL database:

`modernbank_test_db`

**Warning:** Integration tests perform database cleanup. Never configure the test suite to use a development or production database containing important data.

Run all tests:

```powershell
.\mvnw.cmd test
```

**Latest verified local result:**

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Planned Enhancements

- Paginated transaction history
- Account statements
- Additional security and reliability hardening
- Docker containerization
- AWS deployment
- API documentation and examples

## Project Purpose

ModernBank is a backend portfolio project designed to demonstrate practical Java and Spring Boot development, including REST API design, authentication, database modeling, financial transaction consistency, and automated testing.

The project is intended for learning and portfolio demonstration, not real-money banking.

## Author

**Spoorti Kavatage**

GitHub: [Spoortikavatage](https://github.com/Spoortikavatage)