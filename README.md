# ModernBank — Secure Banking Backend API

A Java-based banking backend built withJava 21, Spring Boot 4, Spring Security, JWT, PostgreSQL, and Flyway.

ModernBank is a portfolio project focused on practical backend engineering: secure authentication, account ownership, deposits and withdrawals, account-to-account transfers, transaction history, statement generation, database consistency, concurrency control, automated testing, containerization, and continuous integration.

>Project status: A layered monolithic backend intended for learning and portfolio demonstration. It is not connected to real banking rails and must not be used to hold or transfer real money.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [API reference](#api-reference)
- [Security and reliability](#security-and-reliability)
- [Database and migrations](#database-and-migrations)
- [Run locally](#run-locally)
- [Run with Docker Compose](#run-with-docker-compose)
- [Testing](#testing)
- [Continuous integration](#continuous-integration)
- [Project structure](#project-structure)
- [Planned improvements](#planned-improvements)

## Features

### Authentication and users
- Customer registration and login.
- BCrypt password hashing.
- JWT-based authentication using Spring Security.
- Protected endpoints and authenticated-user identification.
- Retrieve the authenticated customer's profile.
- Request validation and standardized API errors.

### Bank accounts
- Open a savings or other supported account type.
- List accounts belonging to the authenticated customer.
- Account ownership checks for account operations.
- Account status and currency fields.

### Deposits, withdrawals, and history
- Deposit and withdraw funds.
- Reject invalid amounts and withdrawals that exceed the available balance.
- Persist transaction records with a generated reference, transaction type, amount, and balance after the operation.
- Retrieve transaction history using pagination.

### Account-to-account transfers
- Transfer funds between accounts.
- Verify ownership of the source account.
- Reject same-account transfers, inactive accounts, currency mismatches, and insufficient balances.
- Record matching `TRANSFER_OUT` and `TRANSFER_IN` entries.
- Use idempotency keys to make safe retries possible.
- Lock account rows in a consistent order during transfers.

### Bank statements
- Generate a statement for a requested date range.
- Calculate opening balance, total credits, total debits, and closing balance.
- Include transactions in chronological order.
- Limit a statement range to a maximum of three months.
- Check the transaction count before loading statement transactions and reject statements containing more than 5,000 transactions.

### Engineering and operations
- Centralized exception handling and consistent JSON error responses.
- Jakarta Bean Validation for request DTOs.
- Flyway-managed database migrations.
- Integration tests using a separate test database.
- Docker Compose orchestration with persistent PostgreSQL storage.
- GitHub Actions CI workflow.
- Actuator health endpoint.

## Technology stack

| Technology | Usage |
|---|---|
| Java 21 | Application language and runtime |
| Spring Boot 4.1.1 | Application framework |
| Spring Web MVC | REST API endpoints |
| Spring Security | Endpoint security |
| JWT / OAuth2 Resource Server support | Bearer-token authentication |
| BCrypt | Password hashing |
| Spring Data JPA / Hibernate | Persistence and ORM |
| PostgreSQL 16 | Relational database |
| Flyway | Versioned SQL database migrations |
| Jakarta Bean Validation | Request validation |
| Spring Boot Actuator | Application health endpoint |
| Maven Wrapper | Build and dependency management |
| JUnit 5 / Spring test support / MockMvc | Automated tests |
| Docker / Docker Compose | Containerization and local orchestration |
| GitHub Actions | Continuous integration |

## Architecture

ModernBank currently uses a layered monolithic architecture. The application is deployed as one Spring Boot service, with responsibilities separated by layer.

```text
                  Client
                    |
                    v
             REST Controllers
                    |
                    v
               Service Layer
              (Business Logic)
                    |
                    v
             Repository Layer
                    |
                    v
                PostgreSQL

        Spring Security + JWT protects
             secured API requests
```

### Layer responsibilities

- Controller layer: Maps HTTP requests, validates request bodies and parameters, and returns API responses.
- Service layer: Implements banking rules, ownership checks, transaction boundaries, and financial-operation logic.
- Repository layer: Uses Spring Data JPA to read and update persistent entities.
- Entity layer: Represents users, bank accounts, transactions, and transfers.
- DTO layer: Defines request and response payloads independently of persistence entities.
- Security layer: Validates JWT bearer tokens and protects authenticated endpoints.
- Migration layer: Applies ordered SQL schema changes through Flyway.

A monolith was selected first to keep the domain and transaction boundaries understandable. Splitting services into microservices is a possible future evolution, not part of the current implementation.

## API reference

All routes use the `/api/v1` base path. Protected routes require a valid JWT access token:

```http
Authorization: Bearer <access-token>
```

### Endpoint summary

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a customer | Public |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain a JWT | Public |
| `GET` | `/api/v1/users/me` | Get the authenticated user's profile | JWT required |
| `POST` | `/api/v1/accounts` | Open a bank account | JWT required |
| `GET` | `/api/v1/accounts` | List the authenticated user's accounts | JWT required |
| `POST` | `/api/v1/accounts/{accountId}/deposits` | Deposit funds | JWT required |
| `POST` | `/api/v1/accounts/{accountId}/withdrawals` | Withdraw funds | JWT required |
| `GET` | `/api/v1/accounts/{accountId}/transactions` | Get paginated transaction history | JWT required |
| `GET` | `/api/v1/accounts/{accountId}/statement` | Generate an account statement | JWT required |
| `POST` | `/api/v1/transfers` | Transfer funds between accounts | JWT required |

### 1. Register a customer

`POST /api/v1/auth/register`

Example request (illustrative):

```json
{
  "firstName": "Asha",
  "lastName": "Sharma",
  "email": "asha@example.com",
  "password": "Use-A-Strong-Password-Here"
}
```

The registration request validates required fields, email format, field lengths, and password length. The password is stored as a BCrypt hash, not as plain text.

### 2. Login

`POST /api/v1/auth/login`

Example request:

```json
{
  "email": "asha@example.com",
  "password": "Use-A-Strong-Password-Here"
}
```

The login response contains an access token, token type, and token lifetime:

```json
{
  "accessToken": "<access-token>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

The values above are illustrative; the actual token and expiry are generated by the application.

### 3. Open an account

`POST /api/v1/accounts`

Requires authentication.

Example request:

```json
{
  "accountType": "SAVINGS"
}
```

The account type must be one of the enum values supported by the application. The response includes the account ID, account number, account type, currency, balance, status, and creation timestamp.

### 4. List accounts

`GET /api/v1/accounts`

Returns the accounts belonging to the authenticated customer.

### 5. Deposit funds

`POST /api/v1/accounts/{accountId}/deposits`

Example request:

```json
{
  "amount": 1500.00,
  "description": "Initial deposit"
}
```

### 6. Withdraw funds

`POST /api/v1/accounts/{accountId}/withdrawals`

Example request:

```json
{
  "amount": 500.00,
  "description": "Cash withdrawal"
}
```

Deposit and withdrawal amounts are validated as positive monetary values with up to two fractional digits. The service verifies account ownership and account status before applying the operation.

### 7. Get paginated transaction history

`GET /api/v1/accounts/{accountId}/transactions?page=0&size=10`

- `page` is zero-based and defaults to `0`.
- `size` defaults to `10`.
- The accepted page size is from `1` through `100`.

Example:

```http
GET /api/v1/accounts/1/transactions?page=0&size=10
```

The response is a Spring Data `Page` of transaction response objects.

### 8. Generate an account statement

`GET /api/v1/accounts/{accountId}/statement?from=2026-10-01&to=2026-10-31`

Both query parameters use ISO date format (`YYYY-MM-DD`).

The statement response contains:

- Account ID and account number.
- Requested start and end dates.
- Opening balance.
- Total credits.
- Total debits.
- Closing balance.
- Transactions in chronological order.

The implementation limits the requested period to three months. It counts matching transactions before loading them and rejects a statement with more than 5,000 transactions.

### 9. Transfer funds

`POST /api/v1/transfers`

Requires authentication.

Headers:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request body:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 1500.00,
  "description": "Monthly rent",
  "idempotencyKey": "rent-payment-2026-001"
}
```

The `idempotencyKey` is a field in the JSON request body, not a separate HTTP header.

A successful transfer returns `201 Created`. The response includes a generated transfer reference, source and destination account IDs, amount, description, and creation timestamp. Example response (illustrative):

```json
{
  "referenceId": "TRF-EXAMPLE-001",
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 1500.00,
  "description": "Monthly rent",
  "createdAt": "2026-10-20T14:30:00+05:30"
}
```

The same idempotency key with matching transfer details returns the existing transfer rather than applying the transfer again. Reusing the key with different transfer details is rejected.

## Security and reliability

### Authentication and request security
- Passwords are hashed using BCrypt.
- JWT bearer tokens are validated by Spring Security.
- Registration and login are public; the other business endpoints require authentication.
- Authenticated user identity is taken from the validated JWT subject.
- Request DTOs use Jakarta Bean Validation.
- Authentication failures and access-denied responses use JSON handlers.
- Unexpected application exceptions are handled centrally without returning internal exception details to clients.

### Financial consistency
- Deposit, withdrawal, and transfer operations run within database transactions.
- Account rows are locked during financial operations to protect balance updates from concurrent requests.
- Transfers acquire account locks in ascending account-ID order to reduce deadlock risk.
- A transfer produces corresponding outgoing and incoming transaction records.
- A database unique constraint on `(from_account_id, idempotency_key)` adds database-level protection against duplicate transfer requests.
- Account ownership is checked before account operations.

### Logging and health
- Application logging uses INFO as the root level, with application-package DEBUG logging.
- Sensitive values such as passwords, JWTs, and idempotency keys should not be written to logs.
- The Actuator health endpoint is exposed at `/actuator/health`; health details are hidden.

### Docker hardening
The Compose configuration applies several container security settings to the application:
- Runs with a read-only root filesystem.
- Uses a temporary writable `/tmp` filesystem.
- Drops Linux capabilities.
- Disables privilege escalation.
- Limits the number of processes.
- The Dockerfile runs the application as a non-root user.

## Database and migrations

ModernBank uses PostgreSQL 16. Flyway migration scripts are stored in:

```text
src/main/resources/db/migration/
```

The repository currently contains versioned migrations through `V9`. Flyway applies migrations in version order and records applied versions in its schema history table.

The application uses Spring Data JPA and Hibernate for entity persistence. The current `application.properties` uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is the current development configuration. Flyway migrations are also present and track versioned schema changes. For a stricter production deployment, consider switching Hibernate to `validate` after confirming that every schema change is fully managed by Flyway.

### Test database isolation

Integration tests use a separate database named `modernbank_test_db`. The test configuration checks the active database before running destructive test cleanup. Do not point the test profile at a development or production database containing data you need to keep.

## Run locally

### Prerequisites

- JDK 21.
- PostgreSQL 16.
- Git.
- PowerShell on Windows, or a shell on Linux/macOS.

The project includes the Maven Wrapper, so a separate global Maven installation is not required.

### 1. Clone the repository

```bash
git clone https://github.com/Spoortikavatage/modernbank.git
cd modernbank
```

### 2. Create the PostgreSQL database and application user

Create a database named `modernbank_db` and an application user named `modernbank_app`. Grant the application user the permissions needed to connect to the database and use the schema so that Flyway can apply migrations.

For example, using a PostgreSQL administrator account:

```sql
CREATE DATABASE modernbank_db;
CREATE USER modernbank_app WITH PASSWORD 'replace-with-a-strong-password';
GRANT CONNECT, CREATE ON DATABASE modernbank_db TO modernbank_app;
```

Connect to `modernbank_db` and grant schema privileges appropriate to your PostgreSQL setup:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO modernbank_app;
```

If the database or role already exists, do not run the `CREATE` statements again; configure the existing role and database instead.

### 3. Set environment variables

The application reads its database password and JWT signing secret from environment variables:

- `MODERNBANK_DB_PASSWORD`
- `MODERNBANK_JWT_SECRET`

`MODERNBANK_JWT_SECRET` must be a valid Base64-encoded signing secret of sufficient cryptographic strength. Generate a fresh secret for your own environment. Never copy a real secret into this README or commit it to source control.

Windows PowerShell:

```powershell
$env:MODERNBANK_DB_PASSWORD = Read-Host "Database password"
$env:MODERNBANK_JWT_SECRET = Read-Host "Base64 JWT secret"
```

Linux/macOS:

```bash
export MODERNBANK_DB_PASSWORD='your-database-password'
export MODERNBANK_JWT_SECRET='your-base64-jwt-secret'
```

### 4. Run the application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

The application listens on port `8081` by default:

```text
http://localhost:8081
```

Health check:

```text
http://localhost:8081/actuator/health
```

## Run with Docker Compose

### Prerequisites

- Docker Engine or Docker Desktop.
- Docker Compose v2.

### 1. Create a `.env` file

Create `.env` in the project root (the same directory as `compose.yaml`):

```dotenv
POSTGRES_PASSWORD=replace-with-a-strong-database-password
MODERNBANK_JWT_SECRET=replace-with-a-valid-base64-jwt-secret
```

Use unique values for your environment. `.env` is intended for local configuration and must not be committed or shared publicly.

### 2. Create the persistent volume

The Compose file expects an external Docker volume named `modernbank-postgres-data`:

```powershell
docker volume create modernbank-postgres-data
```

This volume stores PostgreSQL data independently of the containers.

### 3. Build and start the services

```powershell
docker compose up -d --build
```

Compose starts PostgreSQL and waits for its health check before starting the application.

### 4. Check container status

```powershell
docker compose ps
```

### 5. Check application health

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

The application is available from the host at:

```text
http://localhost:8082
```

The Compose port mapping is host `8082` to container `8081`. The application connects to PostgreSQL using the Compose service hostname `postgres` on the internal Docker network.

### 6. View application logs

```powershell
docker compose logs -f app
```

### 7. Stop the containers

```powershell
docker compose down
```

This stops and removes the containers and network but preserves the external `modernbank-postgres-data` volume. Do not remove that volume if you need to keep the database contents.

## Testing

Run the complete test suite with the Maven Wrapper.

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

The integration tests cover authentication, account operations, deposits, withdrawals, transaction history, statements, transfers, validation failures, and security/error handling. The test profile uses `modernbank_test_db`, separate from the development database.

### Last recorded verified result

The project history records the following successful test run:

```text
Tests run: 76
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Run the tests again after making changes to confirm the current checkout still passes.

## Continuous integration

The GitHub Actions workflow is located at:

```text
.github/workflows/ci.yml
```

It runs on pushes and pull requests. The workflow:
1. Checks out the repository.
2. Sets up Temurin Java 21.
3. Starts a PostgreSQL 16 service for the tests.
4. Configures the test database and required environment variables.
5. Runs the Maven test suite.

The workflow expects a repository Actions secret named `MODERNBANK_JWT_SECRET`. Configure it in the GitHub repository's Actions secrets; never put the secret value in the workflow file.

## Project structure

```text
modernbank/
├── .github/
│   └── workflows/
│       └── ci.yml
├── scripts/
│   └── smoke-test.ps1
├── src/
│   ├── main/
│   │   ├── java/com/modernbank/modernbank/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── ModernbankApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.properties
│   └── test/
│       ├── java/com/modernbank/modernbank/
│       └── resources/
│           └── application.properties
├── .dockerignore
├── .gitignore
├── Dockerfile
├── compose.yaml
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Planned improvements

The following are possible future enhancements and are not represented as completed features:

- Role-based authorization for different banking roles.
- Refresh-token lifecycle and token revocation.
- OpenAPI / Swagger documentation.
- Additional test coverage and performance testing.
- Application metrics, tracing, and structured observability.
- AWS deployment.
- Scheduled transfers and notifications.
- Potential evolution from the current monolith to independently deployable services.

## Project purpose

ModernBank demonstrates backend concepts relevant to Java developer roles: REST API design, Spring Boot, Spring Security, JWT authentication, relational database design, transaction management, concurrency control, idempotency, automated testing, CI, and Docker security.

It is a learning and portfolio project, not a production banking platform.

## Author

Spoorti Kavatage

GitHub: [Spoortikavatage](https://github.com/Spoortikavatage)
