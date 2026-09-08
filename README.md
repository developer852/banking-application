# 🏛️ Apex Financial • Institutional Banking & Clearing Engine

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Hibernate](https://img.shields.io/badge/ORM-Hibernate%206-blue.svg)](https://hibernate.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208%20%7C%20H2-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Container-Docker%20%26%20Compose-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

> Production-grade, mission-critical backend banking and RTGS clearing system engineered with **Spring Boot 3**, **Hibernate ORM / JPA**, **MySQL**, **Docker**, and an iridescent **Stripe-inspired Tier-1 Institutional Banking Portal** matching the architecture of banking giants like **J.P. Morgan**, **BNY Mellon**, and **HSBC**.

---

## 🌟 Architectural Highlights

### 1. ⚡ Strict ACID Transactions & Deadlock-Free Concurrency
- **Zero Race Conditions**: Financial mutations (deposits, withdrawals, transfers) are executed within `@Transactional(isolation = Isolation.READ_COMMITTED)` boundaries.
- **Deterministic Lock Acquisition**: When settling funds between accounts, the system sorts account entity IDs lexicographically before acquiring `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This mathematical ordering guarantees **zero database deadlocks**, even under high-concurrency bilateral transfers (e.g., Account A ➔ B and Account B ➔ A concurrently).
- **Immutable Ledger Auditing**: Every monetary event writes an immutable transaction log with a unique UUID reference hash, counterparty routing, and post-transaction balance snapshot.

### 2. 🏛️ Tier-1 Institutional Banking & Clearing Portal
- **Stripe-Exact Iridescent Gradient Mesh**: Animated visual design featuring flowing wave gradients, inspired by modern fintech infrastructure.
- **Inter-Bank RTGS Settlement Terminal**: Real-time clearing visualizer connecting clearing members across an automated settlement channel.
- **Interactive Simulation Engine**: One-click demo (`⚡ Run Inter-Bank Settlement Demo`) that onboards Tier-1 entities (J.P. Morgan Treasury & BNY Mellon Custody) and settles a live $125,000.00 wire transfer.

### 3. 🛡️ Enterprise REST API Architecture
- Centralized `@RestControllerAdvice` delivering standardized RFC error structures (`400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Error`).
- Jakarta Bean Validation (`@Valid`, `@DecimalMin`, `@NotNull`, `@Email`).
- OpenAPI 3 / Swagger interactive documentation and H2 database console.

### 4. 🐳 Multi-Stage Docker Containerization
- Lean Alpine-based multi-stage Docker build running as a non-privileged secure user.
- Production `docker-compose.yml` orchestrating MySQL 8 with healthchecks, persistent volumes, and auto-provisioning.

---

## 🏗️ Project Architecture

```text
com.banking
├── config           # OpenAPI 3 specification & Swagger UI configuration
├── controller       # REST API endpoints (Users, Accounts, Transactions)
├── dto              # Strongly typed Request / Response schemas & validation
├── entity           # JPA entities (User, Account, Transaction, Enums)
├── exception        # Domain exceptions & GlobalExceptionHandler
├── repository       # Spring Data JPA repositories with Pessimistic Locking
└── service          # Business services, account number generator, & ACID transfers
```

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 17 LTS**
- **Maven 3.8+**
- **Docker & Docker Compose** (optional for containerized setup)

---

### Option 1: Running Locally (Fast In-Memory Dev Profile)

The application defaults to an embedded H2 database for instant local evaluation:

```bash
# 1. Clone the repository
git clone https://github.com/<username>/banking-application.git
cd banking-application

# 2. Build and run
mvn clean spring-boot:run
```

Once started:
- 🌐 **Institutional Web Portal**: [http://localhost:8080/](http://localhost:8080/)
- 📖 **Swagger OpenAPI UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 🗄️ **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:bankingdb`
  - User: `sa` | Password: *(blank)*

---

### Option 2: Running with Docker Compose & MySQL 8

Deploy both the backend microservice and a production MySQL 8 database:

```bash
docker compose up --build -d
```

- Health checks will ensure MySQL is completely initialized before launching the Spring Boot container.
- Check logs: `docker compose logs -f app`
- Teardown: `docker compose down -v`

---

## 🧪 Automated Testing & Verification

### 1. JUnit 5 & MockMvc Test Suite
Execute the full automated test suite covering unit logic and end-to-end HTTP contracts:

```bash
mvn clean test
```

Test coverage includes:
- ✅ Atomic deposits & withdrawals with balance assertions
- ✅ Overdraft / insufficient balance rejection
- ✅ Bilateral inter-account transfers with lock ordering verification
- ✅ Identity conflict rejection (transferring to same account)
- ✅ Suspended / inactive account protection
- ✅ MockMvc integration flow from user registration to multi-step transfers

### 2. Automated Postman Contract Suite
Import [`Banking_Application.postman_collection.json`](Banking_Application.postman_collection.json) into Postman or execute via Newman CLI:

```bash
npx newman run Banking_Application.postman_collection.json --env-var baseUrl=http://localhost:8080
```

---

## 📡 REST API Contract Reference

| HTTP Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/users` | Register a new customer / corporate entity | `201 Created` |
| `GET` | `/api/v1/users/{id}` | Retrieve customer profile by ID | `200 OK` |
| `GET` | `/api/v1/users` | List all registered customers | `200 OK` |
| `POST` | `/api/v1/accounts` | Open a new bank account with initial reserve | `201 Created` |
| `GET` | `/api/v1/accounts/{accountNumber}` | Retrieve account details & live balance | `200 OK` |
| `GET` | `/api/v1/accounts/user/{userId}` | List accounts associated with a customer | `200 OK` |
| `POST` | `/api/v1/transactions/deposit` | Deposit funds into an account | `200 OK` |
| `POST` | `/api/v1/transactions/withdraw` | Withdraw funds from an account | `200 OK` |
| `POST` | `/api/v1/transactions/transfer` | Execute atomic inter-bank ACID transfer | `200 OK` |
| `GET` | `/api/v1/transactions/account/{accNo}`| Paginated ledger statement & audit trail | `200 OK` |

---

## 📄 License
Distributed under the Apache 2.0 License. See `LICENSE` for more information.
