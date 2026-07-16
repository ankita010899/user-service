# User Service

A production-style **Spring Boot microservice** that manages user identity for a Jira-inspired platform. It exposes a REST API for registration, authentication, and user lifecycle management, persists data in a dedicated PostgreSQL database, secures endpoints with **JWT**, and publishes **Kafka events** when users are deleted.

Built to demonstrate backend microservices skills: layered architecture, stateless security, transactional persistence, and event-driven integration.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring Web MVC |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 15 |
| Messaging | Apache Kafka (Spring Kafka producer) |
| Build | Maven |
| Utilities | Lombok, SLF4J |

---

## Architecture

```text
┌─────────────┐     REST (JWT)      ┌──────────────────┐
│   Client    │ ──────────────────► │  UserController  │
└─────────────┘                     └────────┬─────────┘
                                             │
                                    ┌────────▼─────────┐
                                    │   UserService    │
                                    └────────┬─────────┘
                         ┌───────────────────┼───────────────────┐
                         │                   │                   │
                  ┌──────▼──────┐    ┌───────▼───────┐   ┌───────▼───────┐
                  │UserRepository│    │  JwtService   │   │ KafkaTemplate │
                  └──────┬──────┘    └───────────────┘   └───────┬───────┘
                         │                                        │
                  ┌──────▼──────┐                         ┌───────▼───────┐
                  │ PostgreSQL  │                         │ Kafka Topic:  │
                  │ (users DB)  │                         │user-deleted-  │
                  └─────────────┘                         │    event      │
                                                          └───────────────┘
```

### Package Structure

```text
src/main/java/com/example/jira/
├── UserApplication.java
├── controller/UserController.java
├── service/UserService.java
├── repository/UserRepository.java
├── model/UserEntity.java, UserRole.java
├── dto/RegisterUserRequest, LoginUserRequest, UserResponse, ApiResponse
├── config/KafkaConfig.java
├── config/security/
│   ├── SecurityConfig.java
│   ├── JwtService.java
│   └── JwtAuthenticationFilter.java
└── exception/GlobalExceptionHandler.java, UserNotFoundException.java
```

---

## Features

- **User registration** with BCrypt password hashing and role assignment
- **JWT-based login** returning an access token (1-hour expiry)
- **Stateless API security** via a custom `OncePerRequestFilter`
- **User lookup and deletion** with transactional DB commit before event publish
- **Event-driven deletion** — publishes user ID to Kafka topic `user-deleted-event`
- **Centralized error handling** using RFC 7807 `ProblemDetail` responses

### Supported Roles

`DEVELOPER`, `ADMIN`, `PRODUCT_OWNER`, `QA`

---

## API Reference

**Base URL:** `http://localhost:8084`  
**Prefix:** `/api/user`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/register` | Public | Create a new user |
| `POST` | `/login` | See note* | Authenticate and receive JWT |
| `GET` | `/{id}` | Bearer JWT | Fetch user by ID |
| `DELETE` | `/{id}` | Bearer JWT | Delete user and emit Kafka event |

\* Currently only `/register` is explicitly permitted in `SecurityConfig`. Add `/api/user/login` to `permitAll()` if login should be public without a token.

### Register User

```http
POST /api/user/register
Content-Type: application/json

{
  "username": "john",
  "password": "securePassword123",
  "email": "john@example.com",
  "role": "DEVELOPER"
}
```

**Response (201 Created):**

```json
{
  "message": "User created successfully!",
  "id": "uuid-here",
  "accessToken": "eyJhbG..."
}
```

### Login

```http
POST /api/user/login
Content-Type: application/json

{
  "username": "john",
  "password": "securePassword123"
}
```

**Response (201 Created):**

```json
{
  "message": "Login successful",
  "id": "uuid-here",
  "accessToken": "eyJhbG..."
}
```

### Get User

```http
GET /api/user/{id}
Authorization: Bearer <accessToken>
```

**Response (200 OK):**

```json
{
  "id": "uuid-here",
  "username": "john",
  "email": "john@example.com",
  "role": "DEVELOPER"
}
```

### Delete User

```http
DELETE /api/user/{id}
Authorization: Bearer <accessToken>
```

**Response:** `204 No Content`

On success, a message is published to Kafka topic `user-deleted-event` with the deleted user's ID as the payload.

---

## Security Model

- **Stateless sessions** — `SessionCreationPolicy.STATELESS`
- **CSRF disabled** — appropriate for token-based APIs
- **JWT claims** include username (subject) and role
- **Password storage** — BCrypt via `PasswordEncoder`

Configure the signing key in `application.properties`:

```properties
jwt.secret.key=<your-secret-key>
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure

From the project root:

```bash
docker compose up -d
```

This starts:

| Service | Container | Port |
|---------|-----------|------|
| PostgreSQL (users) | `jira-user-db` | `5433` |
| Kafka (KRaft) | `jira-kafka` | `9092` |

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The service listens on **port 8084**.

### 3. Configuration

Key settings in `src/main/resources/application.properties`:

```properties
server.port=8084
spring.datasource.url=jdbc:postgresql://localhost:5433/jira_users_db
spring.datasource.username=jira_user
spring.datasource.password=secret_pass
spring.kafka.bootstrap-servers=localhost:9092
jwt.secret.key=<secret>
```

---

## Testing

```bash
./mvnw test
```

| Test Type | Location | Focus |
|-----------|----------|-------|
| Unit | `unit/UserServiceUnitTest.java` | Service logic with Mockito |
| Integration | `integration/` | End-to-end API tests (scaffolded) |

---

## Kafka Integration

When a user is deleted, the service:

1. Deletes the record in PostgreSQL
2. Calls `repository.flush()` to commit before messaging
3. Publishes to topic `user-deleted-event`

Downstream services (e.g. **ticket-service**) consume this event for eventual consistency workflows.

---

## Skills Demonstrated

- REST API design with explicit HTTP status codes
- Layered architecture (Controller → Service → Repository)
- Spring Data JPA with PostgreSQL
- JWT authentication and Spring Security filter chains
- Kafka producer integration for domain events
- Global exception handling with `ProblemDetail`
- Dockerized local development environment
- Unit testing with JUnit 5 and Mockito

---

## Roadmap

- [ ] Add `/api/user/login` to public security matchers
- [ ] Replace raw String Kafka payloads with typed `UserDeletedEvent` DTOs
- [ ] Complete integration test coverage for register, login, and delete flows
- [ ] Externalize secrets via environment variables or a secrets manager
