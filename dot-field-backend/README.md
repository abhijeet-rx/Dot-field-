# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.

---

## Requirements

| Tool       | Version |
|------------|---------|
| Java       | 21      |
| Maven      | 3.9+    |
| PostgreSQL | 14+     |

> **Note:** Maven is bundled via the Maven Wrapper (`mvnw` / `mvnw.cmd`), so a global Maven installation is not required.

---

## Setup

### 1. Clone the project

```bash
git clone <repository-url>
cd Dot-field-/dot-field-backend
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE dot_field;
```

### 3. Configure environment variables

Copy the example file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/dot_field
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**On Windows**, set the environment variables before running:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/dot_field"
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

Or use your IDE's run configuration to inject them.

### 4. Run the application

```bash
# Unix / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8080**.

### 5. Test the health endpoint

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "data": {
    "status": "UP"
  },
  "message": "Success"
}
```

### 6. Run tests

Tests use an in-memory H2 database — no PostgreSQL required.

```bash
.\mvnw.cmd clean test
```

---

## Project Structure

```
dot-field-backend/
├── src/main/java/com/dotfield/
│   ├── DotFieldApplication.java    # Spring Boot entry point
│   ├── controller/                 # REST controllers (thin — delegate to services)
│   ├── service/                    # Business logic layer
│   ├── repository/                 # Spring Data JPA repositories
│   ├── entity/                     # JPA entities (database models)
│   ├── dto/                        # Data Transfer Objects (API boundaries)
│   ├── mapper/                     # Entity ↔ DTO conversion
│   ├── exception/                  # Global error handling
│   └── config/                     # Spring configuration beans
├── src/main/resources/
│   └── application.properties      # App configuration (uses env vars)
├── src/test/                       # Tests (JUnit 5 + Spring Boot Test)
├── pom.xml                         # Maven dependencies
├── .env.example                    # Environment variable template
└── .gitignore
```

### Architecture

```
Controller → Service → Repository → PostgreSQL
```

- **Controllers** are thin — they validate input and delegate to services.
- **Services** contain all business logic.
- **Repositories** handle persistence only.
- **DTOs** are used at API boundaries; JPA entities are never exposed directly.

---

## API Response Format

### Success

```json
{
  "data": { ... },
  "message": "Success"
}
```

### Error

```json
{
  "status": 404,
  "message": "Resource not found",
  "timestamp": "2025-01-01T12:00:00"
}
```

### Validation Error

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-01-01T12:00:00",
  "errors": {
    "email": "must not be blank",
    "name": "size must be between 2 and 100"
  }
}
```

---

## Current Phase

```
Phase 1 — Backend Foundation
Status: Complete
```

### What's included in Phase 1

- ✅ Spring Boot 3.4.1 + Java 21 + Maven
- ✅ PostgreSQL connection via environment variables
- ✅ Spring Data JPA configured
- ✅ Health endpoint (`GET /api/health`)
- ✅ Global exception handling
- ✅ Consistent API response structure
- ✅ Jakarta Bean Validation infrastructure
- ✅ Lombok integration
- ✅ Application context and health endpoint tests

---

## Future Phases

| Phase | Focus                     |
|-------|---------------------------|
| 2     | Candidate Profile         |
| 3     | Job Management            |
| 4     | Job Extraction            |
| 5     | Job Analysis & Matching   |
| 6     | Resume Tailoring          |

These phases will be implemented incrementally in future iterations.

---

## License

Private project — not for distribution.
