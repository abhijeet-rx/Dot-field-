# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.
> Phase 3 implements the Job Management subsystem.

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
│   ├── repository/                 # Spring Data JPA repositories & specifications
│   ├── entity/                     # JPA entities & enums (database models)
│   ├── dto/                        # Data Transfer Objects & PagedResponse wrapper
│   ├── mapper/                     # Entity ↔ DTO conversion
│   ├── exception/                  # Global error handling
│   └── config/                     # Spring configuration beans
├── src/main/resources/
│   └── application.properties      # App configuration (uses env vars)
├── src/test/                       # Unit & Integration tests (JUnit 5 + Mockito + MockMvc)
├── pom.xml                         # Maven dependencies
├── .env.example                    # Environment variable template
└── .gitignore
```

### Architecture

```
Controller → Service → Repository / Specification → PostgreSQL
```

- **Controllers** are thin — they validate input and delegate to services.
- **Services** contain all business logic, transaction boundaries, and salary validations.
- **Repositories & Specifications** handle dynamic query filtering and pagination.
- **DTOs** are used at API boundaries; JPA entities are never exposed directly.

---

## API Documentation — Phase 2 Candidate Profile

### Base Path: `/api`

### Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile` | Retrieve the complete candidate profile |
| `PUT`  | `/api/profile` | Create or update candidate profile basic details |

#### Update Profile Request Example (`PUT /api/profile`)

```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "linkedinUrl": "https://linkedin.com/in/janedoe",
  "githubUrl": "https://github.com/janedoe",
  "portfolioUrl": "https://janedoe.dev"
}
```

---

### Skills Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/skills` | List candidate skills |
| `POST` | `/api/profile/skills` | Add a new skill to candidate profile |
| `DELETE` | `/api/profile/skills/{id}` | Delete a skill |

#### Add Skill Request Example (`POST /api/profile/skills`)

```json
{
  "name": "Java",
  "category": "LANGUAGE"
}
```

Categories: `LANGUAGE`, `FRONTEND`, `BACKEND`, `DATABASE`, `TOOL`, `FRAMEWORK`, `OTHER`

---

### Education Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/education` | List education records |
| `POST` | `/api/profile/education` | Add education record |
| `PUT`  | `/api/profile/education/{id}` | Update education record |
| `DELETE` | `/api/profile/education/{id}` | Delete education record |

---

### Project Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/projects` | List candidate projects |
| `POST` | `/api/profile/projects` | Add project record |
| `PUT`  | `/api/profile/projects/{id}` | Update project record |
| `DELETE` | `/api/profile/projects/{id}` | Delete project record |

---

### Experience Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/experience` | List candidate work experience |
| `POST` | `/api/profile/experience` | Add experience record |
| `PUT`  | `/api/profile/experience/{id}` | Update experience record |
| `DELETE` | `/api/profile/experience/{id}` | Delete experience record |

---

## API Documentation — Phase 3 Job Management

### Base Path: `/api`

### Job Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST`   | `/api/jobs` | Create a new job opportunity |
| `GET`    | `/api/jobs` | List jobs with pagination & composable filters |
| `GET`    | `/api/jobs/{id}` | Retrieve a single job opportunity by ID |
| `PUT`    | `/api/jobs/{id}` | Complete update of a job opportunity |
| `PATCH`  | `/api/jobs/{id}/status` | Update job status |
| `DELETE` | `/api/jobs/{id}` | Delete a job opportunity |

---

### Query Parameters for `GET /api/jobs`

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | `int` | `0` | Zero-indexed page number |
| `size` | `int` | `20` | Page size |
| `status` | `JobStatus` | `null` | Exact status match (`SAVED`, `APPLIED`, `REJECTED`, `INTERVIEW`, `OFFER`, `ARCHIVED`) |
| `company` | `String` | `null` | Partial, case-insensitive company name search |
| `source` | `String` | `null` | Exact case-insensitive source match (e.g. `LINKEDIN`, `MANUAL`) |
| `remoteType` | `RemoteType` | `null` | Exact remote type match (`ONSITE`, `HYBRID`, `REMOTE`, `OTHER`) |
| `employmentType` | `EmploymentType` | `null` | Exact employment type match (`FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERNSHIP`, `TEMPORARY`, `OTHER`) |

#### Example Paginated & Filtered Query

```http
GET /api/jobs?page=0&size=20&status=SAVED&company=google&remoteType=REMOTE
```

---

### Request & Response Examples

#### Create Job Request (`POST /api/jobs`)

```json
{
  "title": "Software Engineer",
  "company": "Google",
  "location": "Mountain View, CA",
  "description": "Backend platform engineering",
  "jobUrl": "https://careers.google.com/jobs/123",
  "source": "LINKEDIN",
  "employmentType": "FULL_TIME",
  "remoteType": "HYBRID",
  "status": "SAVED",
  "salaryMin": 140000.00,
  "salaryMax": 190000.00,
  "currency": "USD",
  "postedDate": "2026-08-15"
}
```

#### Successful Paginated Response (`GET /api/jobs?page=0&size=20`)

```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Software Engineer",
        "company": "Google",
        "location": "Mountain View, CA",
        "description": "Backend platform engineering",
        "jobUrl": "https://careers.google.com/jobs/123",
        "source": "LINKEDIN",
        "employmentType": "FULL_TIME",
        "remoteType": "HYBRID",
        "status": "SAVED",
        "salaryMin": 140000.00,
        "salaryMax": 190000.00,
        "currency": "USD",
        "postedDate": "2026-08-15",
        "createdAt": "2026-08-30T12:00:00",
        "updatedAt": "2026-08-30T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "message": "Jobs retrieved successfully"
}
```

#### Update Status Request (`PATCH /api/jobs/1/status`)

```json
{
  "status": "APPLIED"
}
```

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
  "timestamp": "2026-08-30T12:00:00"
}
```

### Validation Error

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-08-30T12:00:00",
  "errors": {
    "title": "Job title is required",
    "company": "Company name is required"
  }
}
```

---

## Current Phase

```
Phase 3 — Job Management
Status: Complete
```

### What's included in Phase 3

- ✅ Job domain model (`Job` JPA entity mapped to `jobs` PostgreSQL table)
- ✅ `EmploymentType`, `RemoteType`, and `JobStatus` enums with `@Enumerated(EnumType.STRING)`
- ✅ Extensible string-based `source` representation with `"MANUAL"` default and `"OTHER"` semantic distinction
- ✅ JPA lifecycle listeners (`@PrePersist`, `@PreUpdate`) for timestamps (`createdAt`, `updatedAt`) and defaults
- ✅ DTO Isolation Layer (`JobResponse`, `PagedResponse<T>`, `CreateJobRequest`, `UpdateJobRequest`, `UpdateJobStatusRequest`)
- ✅ Bean Validation for incoming requests and business range validation (`salaryMin <= salaryMax`)
- ✅ Spring Data JPA `JobRepository` & composable `JobSpecification` dynamic filters
- ✅ `JobMapper` component for bidirectional DTO ↔ Entity conversion
- ✅ Transactional `JobService` covering CRUD, status updates, pagination, and error handling
- ✅ Thin `JobController` REST endpoints under `/api/jobs`
- ✅ Service unit tests with Mockito (`JobServiceTest`)
- ✅ Controller integration test suite (`JobControllerTest`) with MockMvc and H2 (45 total passing tests across Phase 1, 2, and 3)

---

## Future Phases

| Phase | Focus                     | Status       |
|-------|---------------------------|--------------|
| 1     | Backend Foundation        | ✅ Complete  |
| 2     | Candidate Profile         | ✅ Complete  |
| 3     | Job Management            | ✅ Complete  |
| 4     | Job Extraction            | ⏳ Next      |
| 5     | Job Analysis & Matching   | ⏳           |
| 6     | Resume Tailoring          | ⏳           |

---

## License

Private project — not for distribution.
