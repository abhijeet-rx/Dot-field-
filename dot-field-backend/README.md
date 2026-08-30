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

#### Source Adapter (V1 — Simulated)

Phase 7 V1 currently uses a **controlled/simulated source adapter** (`CompanyCareerPageSource`). It returns a fixed dataset of sample job listings for development and testing purposes. **It does not scrape arbitrary company career pages, public feeds, LinkedIn, Indeed, Naukri, Glassdoor, or any external website.**

The pluggable `JobSource` interface and `JobSourceRegistry` support adding real adapters in future phases without modifying the discovery orchestration code.

#### Supported Request Fields

| Field | Type | Supported by V1 Adapter | Description |
|-------|------|------------------------|-------------|
| `source` | String (required) | ✅ | Source adapter name (V1: `COMPANY_WEBSITE` only) |
| `keyword` | String | ✅ Filtered against dataset | Case-insensitive substring match on title and description |
| `location` | String | ✅ Filtered against dataset | Case-insensitive substring match on listing location |
| `company` | String | ✅ Filtered against dataset | Case-insensitive substring match on listing company name |
| `maxResults` | Integer (1–100) | ✅ Bounds results | Maximum number of listings returned |
| `employmentType` | Enum | ❌ Ignored | Each listing has its own fixed employment type; filter not applied |
| `remoteType` | Enum | ❌ Ignored | Each listing has its own fixed remote type; filter not applied |

#### Shared Extraction & Normalization Pipeline

Phase 7 discovery reuses the exact same extraction and normalization core (`JobExtractionPipeline`, `JobNormalizationUtil`) as Phase 4 manual extraction, with zero code duplication.

#### 3-Level Deterministic Deduplication

Identity precedence (stronger levels take priority):

1. **Level 1 — Source + External ID:** Identity key `(source, externalId)`. Authoritative when both are present. A match at Level 1 stops further deduplication checks.
2. **Level 2 — Canonical URL:** Canonical URL computed from the raw job URL. A match at Level 2 stops further checks.
3. **Level 3 — Composite SHA-256 Fingerprint:** Hash of `norm(company) + "|" + norm(title) + "|" + norm(location) [+ "|" + norm(description)]`. Only generated when all three minimum fields (company, title, location) are present. If any is missing/blank, no fingerprint is generated (prevents weak deduplication).

#### Canonical URL Normalization

- Scheme and hostname lowercased
- Default ports removed (`:80` for HTTP, `:443` for HTTPS)
- Trailing slashes removed (except root `/`)
- Known tracking parameters stripped: `utm_source`, `utm_medium`, `utm_campaign`, `utm_term`, `utm_content`, `ref`, `fbclid`
- Functional query parameters preserved (e.g., `location=bangalore`)
- `http://` and `https://` remain **distinct** (no scheme equivalence)

#### Database Uniqueness & Concurrency Protection

- **`(source, externalId)`** — enforced by JPA `@UniqueConstraint` (database-level unique constraint). Prevents duplicate rows when both values are present.
- **`canonicalUrl`** — enforced by JPA `@UniqueConstraint` (database-level unique constraint). Prevents duplicate rows when canonical URL is present.
- **`deduplicationFingerprint`** — normal index (not unique). Used for application-level deduplication but does not block inserts.
- **Concurrent duplicate protection:** Each listing is persisted in its own `REQUIRES_NEW` transaction via `JobDiscoveryPersistenceHelper`. If a concurrent insert triggers a unique constraint violation (`DataIntegrityViolationException`), the inner transaction rolls back cleanly and the service re-fetches the existing record in a new clean context. No unexplained 500 errors.

#### Discovery Statistics

All counters are tracked **explicitly** (not derived indirectly):

| Counter | Definition |
|---------|------------|
| `newJobs` | Listing that created a new database Job |
| `updatedJobs` | Existing Job matched, at least one source-owned field changed |
| `unchangedJobs` | Existing Job matched, no source-owned fields changed |
| `duplicates` | Within-batch duplicate listings (same externalId appearing twice in the same batch) |
| `failed` | Listing that could not be processed (extraction/persistence error) |

Failures are never counted as duplicates.

#### Refresh Semantics & Status Preservation

When an existing job is re-discovered:
- **Source-owned fields updated:** description, salary, URL, employment type, remote type, posted date, canonical URL, fingerprint
- **`lastDiscoveredAt` updated:** Timestamp reflecting when discovery last observed this listing
- **`createdAt` unchanged:** First persistence timestamp
- **`updatedAt` changed only if source-owned fields actually changed**
- **`job.getStatus()` NEVER modified:** Candidate tracking status (`SAVED`, `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, `ARCHIVED`) is strictly preserved across all discovery refreshes

> **Note:** The current `JobStatus` model does not distinguish automatically discovered jobs from manually saved jobs, so V1 uses `SAVED` for newly discovered jobs.

#### Source Failure Behavior

If a source fails (network error, parser error, timeout, rate limit):
- Existing jobs are **never deleted or expired**
- `lastDiscoveredAt` is **not falsely updated** for listings that failed to process
- The response returns `failed=1` at the source level

#### Scheduler

Background scheduling infrastructure exists (`JobDiscoveryScheduler`) but is **inactive by default**:
- `job-discovery.scheduler.enabled=false` (safe default)
- `@EnableScheduling` is not declared — the `@Scheduled` annotation is non-functional even if the bean were loaded
- V1 uses manual `POST /api/jobs/discover` as the primary discovery mechanism
- Scheduling activation is deferred to a future phase

#### Security Restrictions

- No arbitrary user-supplied URLs accepted (configured source destinations only)
- `maxResults` bounded to 1–100
- No CAPTCHA bypass, bot detection bypass, proxy rotation, or credential harvesting implemented
- No real external network calls in V1

#### Example Discovery Request & Response (`POST /api/jobs/discover`)

**Request:**
```json
{
  "source": "COMPANY_WEBSITE",
  "keyword": "Java Backend Developer",
  "location": "Bangalore",
  "maxResults": 20
}
```

**Response:**
```json
{
  "data": {
    "discovered": 1,
    "newJobs": 1,
    "updatedJobs": 0,
    "unchangedJobs": 0,
    "duplicates": 0,
    "failed": 0,
    "sourceResults": [
      {
        "source": "COMPANY_WEBSITE",
        "discovered": 1,
        "newJobs": 1,
        "updatedJobs": 0,
        "unchangedJobs": 0,
        "duplicates": 0,
        "failed": 0
      }
    ]
  },
  "message": "Job discovery completed successfully"
}
```

#### Known Limitations (V1)

- Only one source adapter (`COMPANY_WEBSITE`) is available, and it returns simulated data
- `employmentType` and `remoteType` request filters are not applied by the V1 adapter
- No real external HTTP fetching occurs
- Background scheduling is prepared but inactive
- `JobStatus` does not distinguish discovered jobs from manually saved jobs

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
- ✅ `CompanyCareerPageSource` V1 simulated adapter for development and testing (`COMPANY_WEBSITE`)
- ✅ Refactored shared Phase 4 `JobExtractionPipeline` for zero normalization code duplication
- ✅ `Job.java` entity enhancements: `externalId`, `canonicalUrl`, `deduplicationFingerprint`, `lastDiscoveredAt`
- ✅ Database-level unique constraints on `(source, externalId)` and `canonicalUrl` via JPA `@UniqueConstraint`
- ✅ 3-level deterministic deduplication engine (`JobDeduplicationService`): Level 1 (External ID) → Level 2 (Canonical URL) → Level 3 (SHA-256 Fingerprint)
- ✅ Conservative URL canonicalization (stripping tracking params, preserving functional params, keeping `http`/`https` distinct)
- ✅ Fingerprint safety: no weak fingerprint generated when company/title/location is missing
- ✅ Concurrent duplicate protection via `REQUIRES_NEW` transactional boundary with `DataIntegrityViolationException` re-fetch
- ✅ Explicit discovery statistics (newJobs, updatedJobs, unchangedJobs, duplicates, failed) — failures never counted as duplicates
- ✅ Strict candidate application status preservation (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, etc. preserved on external refresh)
- ✅ 100% idempotent repeated discovery execution (verified by integration tests)
- ✅ Fail-safe source handling without accidental job deletion or expiration
- ✅ Configurable background scheduler infrastructure (disabled by default, deferred for future activation)
- ✅ REST API controller (`JobDiscoveryController`) exposing `POST /api/jobs/discover`
- ✅ Comprehensive unit & integration tests including H2 persistence-level idempotency, concurrency, and status preservation tests

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
