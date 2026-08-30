# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.
> Phase 3 implements the Job Management subsystem.
> Phase 4 implements the Job Extraction & Ingestion subsystem.

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
│   ├── service/                    # Business logic & extraction services
│   ├── extractor/                  # JobExtractor Strategy, Normalization & DTOs
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
Controller → Extraction/Business Service → Extractor Registry / Repository → PostgreSQL
```

- **Controllers** are thin — they validate input and delegate to services.
- **Extraction Services** orchestrate extraction strategy lookup, field normalization, validation, and persistence.
- **Extractor Strategy** decouples job sources (`COMPANY_WEBSITE`) from input formats (`Map<String, Object>`).
- **Repositories & Specifications** handle dynamic query filtering and pagination.
- **DTOs** isolate raw external data (`ExtractJobRequest`, `ExtractedJob`) from the internal domain model (`Job`).

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

### Ingestion Details & Extractor Support

- **Job Source vs. Input Format:** `source` (e.g. `"COMPANY_WEBSITE"`) represents listing origin. Raw data payload `rawData` is transmitted in JSON key-value structure (`Map<String, Object>`).
- **Supported Sources:** Currently supported source adapter is `"COMPANY_WEBSITE"` (`CompanyWebsiteJobExtractor`).
- **Unsupported Source Behavior:** Requests containing unsupported sources (e.g. `"LINKEDIN"`, `"INDEED"`) immediately return `400 Bad Request` with message `"Unsupported job source: LINKEDIN"`.
- **Normalization:**
  - **Text:** Trims surrounding whitespace and converts blank strings to `null`.
  - **Source:** Converts source strings to uppercase (`"COMPANY_WEBSITE"`).
  - **Employment Type:** Maps `"full time"`, `"part time"`, `"contract"`, `"internship"`, `"temporary"` to `EmploymentType` enums (defaults to `OTHER`).
  - **Remote Type:** Maps `"remote"`, `"hybrid"`, `"onsite"` to `RemoteType` enums (defaults to `OTHER`).
  - **Salary:** Conservatively parses ranges (e.g. `"$120,000 - $150,000"`, `"₹10,00,000 - ₹15,00,000"`); returns `null` for unparseable/vague text (`"Competitive salary"`).
  - **Date:** Parses ISO strings (`"YYYY-MM-DD"`); relative strings (`"Posted 2 days ago"`) return `null`.
- **Duplicate Policy:** If a job with matching `(source, jobUrl)` already exists, a warning is logged and ingestion continues cleanly without overwriting or deleting existing records.

#### Extract Job Request Example (`POST /api/jobs/extract`)

```json
{
  "source": "COMPANY_WEBSITE",
  "rawData": {
    "title": "Backend Engineer",
    "company": "Example Corp",
    "location": "Bangalore, India",
    "description": "Building Java 21 microservices platform",
    "jobUrl": "https://example.com/jobs/123",
    "employmentType": "Full Time",
    "remoteType": "Remote",
    "salary": "$120,000 - $150,000",
    "postedDate": "2026-08-01"
  }
}
```

#### Successful Ingestion Response

```json
{
  "data": {
    "id": 1,
    "title": "Backend Engineer",
    "company": "Example Corp",
    "location": "Bangalore, India",
    "description": "Building Java 21 microservices platform",
    "jobUrl": "https://example.com/jobs/123",
    "source": "COMPANY_WEBSITE",
    "employmentType": "FULL_TIME",
    "remoteType": "REMOTE",
    "status": "SAVED",
    "salaryMin": 120000.00,
    "salaryMax": 150000.00,
    "currency": "USD",
    "postedDate": "2026-08-01",
    "createdAt": "2026-08-30T12:44:00",
    "updatedAt": "2026-08-30T12:44:00"
  },
  "message": "Job opportunity extracted and ingested successfully"
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

### Validation / Extraction Error

```json
{
  "status": 400,
  "message": "Unsupported job source: LINKEDIN",
  "timestamp": "2026-08-30T12:00:00"
}
```

---

## Current Phase

```
Phase 4 — Job Extraction & Ingestion
Status: Complete
```

### What's included in Phase 4

- ✅ Conceptual separation between Job Source (`source`) and Input Format (`Map<String, Object>`)
- ✅ `JobExtractor` strategy interface for modular extraction adapters
- ✅ Extractor Registry using Spring DI (`List<JobExtractor> extractors`)
- ✅ `CompanyWebsiteJobExtractor` component supporting `"COMPANY_WEBSITE"` raw JSON inputs
- ✅ HTTP 400 Bad Request error handling for unsupported sources (`"Unsupported job source: LINKEDIN"`)
- ✅ `ExtractedJob` intermediate normalized DTO isolating external data from JPA entities
- ✅ Centralized `JobNormalizationUtil` for text trimming, source upper-casing, employment type, remote type, conservative salary parsing, and ISO date parsing
- ✅ `JobExtractionService` orchestrating extraction, validation (`title`, `company`, `source`), salary range validation (`salaryMin <= salaryMax`), duplicate warning logging, mapping, and persistence
- ✅ `POST /api/jobs/extract` REST endpoint in `JobController`
- ✅ Reused Phase 3 `Job` entity, `JobRepository`, `JobMapper`, `ApiResponse`, and `GlobalExceptionHandler`
- ✅ Comprehensive unit and integration test suite (`JobNormalizationUtilTest`, `CompanyWebsiteJobExtractorTest`, `JobExtractionServiceTest`, `JobExtractionControllerTest`)
- ✅ 74 total passing automated tests across Phase 1, 2, 3, and 4

---

## Future Phases

| Phase | Focus                     | Status       |
|-------|---------------------------|--------------|
| 1     | Backend Foundation        | ✅ Complete  |
| 2     | Candidate Profile         | ✅ Complete  |
| 3     | Job Management            | ✅ Complete  |
| 4     | Job Extraction & Ingestion| ✅ Complete  |
| 5     | Job Analysis & Matching   | ⏳ Next      |
| 6     | Resume Tailoring          | ⏳           |

---

## License

Private project — not for distribution.
