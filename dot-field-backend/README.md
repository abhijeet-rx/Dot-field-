# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.
> Phase 3 implements the Job Management subsystem.
> Phase 4 implements the Job Extraction & Ingestion subsystem.
> Phase 5 implements the Job Analysis & Matching subsystem.
> Phase 6 implements the Resume Tailoring subsystem.
> Phase 7 implements the Job Discovery & Aggregation subsystem.

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
│   ├── service/                    # Business, extraction, matching & discovery services
│   ├── discovery/                  # Job Discovery: Sources, Registry, Deduplication & Scheduler
│   │   └── source/                 # Pluggable Source Adapters (e.g. CompanyCareerPageSource)
│   ├── extractor/                  # JobExtractor Strategy, Shared Pipeline, Normalization & DTOs
│   ├── matching/                   # Matching Engine: Normalization, Extractors, Matchers & Calculators
│   ├── tailoring/                  # Tailoring Engine: Keyword Selection, Prioritization & Section Building
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

---

## API Documentation — Phase 2 Candidate Profile

### Base Path: `/api`

### Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile` | Retrieve the complete candidate profile |
| `PUT`  | `/api/profile` | Create or update candidate profile basic details |

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

## API Documentation — Phase 4 Job Extraction & Ingestion

### Base Path: `/api`

### Extraction Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/jobs/extract` | Extract and ingest raw job listing data into Job domain model |

---

## API Documentation — Phase 5 Job Analysis & Matching

### Base Path: `/api`

### Matching Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/jobs/{id}/match` | Analyze candidate profile match against job opportunity |

---

## API Documentation — Phase 6 Resume Tailoring

### Base Path: `/api`

### Tailoring Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/jobs/{id}/resume/tailor` | Generate a job-specific tailored resume representation |

---

## API Documentation — Phase 7 Job Discovery & Aggregation

### Base Path: `/api`

### Discovery Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/jobs/discover` | Discover, normalize, deduplicate, and aggregate job listings from supported sources |

---

### Discovery Architecture & Rules

- **Pluggable Source Strategy (`JobSource` & `JobSourceRegistry`):** External sources are encapsulated in pluggable adapters (`JobSource`). `JobSourceRegistry` resolves supported adapters dynamically without discovery orchestrator code modifications.
- **V1 Source Adapter (`CompanyCareerPageSource`):** Supports configured public feeds and company career listings (`COMPANY_WEBSITE`). Does **not** perform arbitrary user URL fetching, avoiding SSRF security vulnerabilities.
- **Shared Extraction & Normalization Pipeline (`JobExtractionPipeline`):** Phase 7 discovery reuses the exact same extraction and normalization core (`JobNormalizationUtil`) as Phase 4 manual extraction without code duplication.
- **3-Level Deterministic Deduplication (`JobDeduplicationService`):**
  - **Level 1 — Source + External ID:** Identity key `(source, externalId)` for provider-assigned IDs.
  - **Level 2 — Canonical URL:** Scheme/host lowercased, default ports removed, trailing slashes removed, tracking params (`utm_*`, `ref`, `fbclid`) stripped. Functional params (e.g. `location=bangalore`) are preserved. `http` and `https` remain distinct.
  - **Level 3 — Composite SHA-256 Fingerprint:** Hashed from `company + title + location (+ description)`. Skipped if minimum company/title/location fields are absent.
- **Concurrency & Race Condition Protection:** PostgreSQL unique indexes on `(source, externalId)` and `canonicalUrl`. `DataIntegrityViolationException` is caught during concurrent saves and handled by re-fetching and updating existing records.
- **User Application State Preservation:** When an external listing refresh occurs, externally sourced listing fields (`description`, `salaryMin`, `salaryMax`, `jobUrl`, `lastDiscoveredAt`, etc.) are updated, but **`job.getStatus()` is strictly PRESERVED** across all candidate tracking states (`SAVED`, `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, `ARCHIVED`).
- **Idempotency & Fail-Safety:** Repeated discovery runs yield 0 new jobs, N unchanged. Source failures do not delete or expire existing jobs.
- **Configurable Background Scheduler (`JobDiscoveryScheduler`):** Disabled by default (`job-discovery.scheduler.enabled=false`). Includes an execution lock (`AtomicBoolean isRunning`) to prevent overlapping runs when enabled.

#### Example Discovery Request & Response (`POST /api/jobs/discover`)

**Request:**
```json
{
  "source": "COMPANY_WEBSITE",
  "keyword": "Java Backend Developer",
  "location": "Bangalore",
  "remoteType": "REMOTE",
  "maxResults": 20
}
```

**Response:**
```json
{
  "data": {
    "discovered": 2,
    "newJobs": 1,
    "updatedJobs": 1,
    "unchangedJobs": 0,
    "duplicates": 0,
    "failed": 0,
    "sourceResults": [
      {
        "source": "COMPANY_WEBSITE",
        "discovered": 2,
        "newJobs": 1,
        "updatedJobs": 1,
        "unchangedJobs": 0,
        "failed": 0
      }
    ]
  },
  "message": "Job discovery completed successfully"
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

---

## Current Phase

```
Phase 7 — Job Discovery & Aggregation
Status: Complete
```

### What's included in Phase 7

- ✅ Pluggable `JobSource` strategy & `JobSourceRegistry`
- ✅ `CompanyCareerPageSource` V1 adapter for configured public feeds (`COMPANY_WEBSITE`)
- ✅ Refactored shared Phase 4 `JobExtractionPipeline` for zero normalization code duplication
- ✅ `Job.java` entity enhancements: `externalId`, `canonicalUrl`, `deduplicationFingerprint`, `lastDiscoveredAt`
- ✅ PostgreSQL unique indexes on `(source, externalId)` and `canonicalUrl` for concurrency race-condition protection
- ✅ 3-level deterministic deduplication engine (`JobDeduplicationService`): Level 1 (External ID) $\to$ Level 2 (Canonical URL) $\to$ Level 3 (SHA-256 Fingerprint)
- ✅ Conservative URL canonicalization (stripping `utm_*`, preserving functional params, keeping `http`/`https` distinct)
- ✅ Strict candidate application status preservation (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, etc. preserved on external refresh)
- ✅ 100% idempotent repeated discovery execution
- ✅ Fail-safe source handling without accidental job deletion or expiration
- ✅ Configurable background scheduler (`JobDiscoveryScheduler`) disabled by default
- ✅ REST API controller (`JobDiscoveryController`) exposing `POST /api/jobs/discover`
- ✅ Comprehensive unit & integration tests (`JobSourceTest`, `CompanyCareerPageSourceTest`, `JobDeduplicationServiceTest`, `JobDiscoveryServiceTest`, `JobDiscoveryControllerTest`)
- ✅ 163 total passing automated tests across Phase 1, 2, 3, 4, 5, 6, and 7

---

## Phase Roadmap

| Phase | Focus                     | Status       |
|-------|---------------------------|--------------|
| 1     | Backend Foundation        | ✅ Complete  |
| 2     | Candidate Profile         | ✅ Complete  |
| 3     | Job Management            | ✅ Complete  |
| 4     | Job Extraction & Ingestion| ✅ Complete  |
| 5     | Job Analysis & Matching   | ✅ Complete  |
| 6     | Resume Tailoring          | ✅ Complete  |
| 7     | Job Discovery & Aggregation| ✅ Complete |

---

## License

Private project — not for distribution.
