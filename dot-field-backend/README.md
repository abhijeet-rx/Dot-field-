# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.
> Phase 3 implements the Job Management subsystem.
> Phase 4 implements the Job Extraction & Ingestion subsystem.
> Phase 5 implements the Job Analysis & Matching subsystem.

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
│   ├── service/                    # Business, extraction & matching services
│   ├── extractor/                  # JobExtractor Strategy, Normalization & DTOs
│   ├── matching/                   # Matching Engine: Normalization, Extractors, Matchers & Calculators
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

### Matching Engine Architecture & Rules

- **Deterministic & Explainable:** Matching is 100% deterministic based on heuristic extraction, token normalization, and mathematical scoring. No LLMs or vector databases are used.
- **Scoring Range:** `0 <= overallScore <= 100` (integer rounded).
- **Match Categories:**
  - `80 – 100` → `STRONG_MATCH`
  - `60 – 79`  → `GOOD_MATCH`
  - `40 – 59`  → `PARTIAL_MATCH`
  - `0 – 39`   → `WEAK_MATCH`
- **Default Dimension Weights:**
  - Skill Score: **60%**
  - Experience Score: **20%**
  - Education Score: **10%**
  - Location/Remote Score: **10%**
- **Dynamic Weight Redistribution:**
  - If a dimension is `UNKNOWN` or `NOT_REQUIRED`, its weight is excluded from the denominator ($S_{\text{available}} = \sum_{d \in \text{Available}} W_d$) and remaining weights are normalized ($W_d^{\text{normalized}} = W_d / S_{\text{available}}$) so candidate scores are not penalized.
  - If all dimensions are `UNKNOWN`, `overallScore = 0` and `matchCategory = WEAK_MATCH`.
- **Dynamic On-Demand Calculation:** Match results are generated dynamically on demand and are not stored in the database.

#### Example Match Response (`GET /api/jobs/1/match`)

```json
{
  "data": {
    "jobId": 1,
    "profileId": 1,
    "overallScore": 88,
    "matchCategory": "STRONG_MATCH",
    "skillScore": 80,
    "experienceScore": 100,
    "educationScore": 100,
    "locationScore": 100,
    "matchedRequiredSkills": [
      "java",
      "spring boot"
    ],
    "missingRequiredSkills": [
      "postgresql"
    ],
    "matchedPreferredSkills": [
      "docker"
    ],
    "missingPreferredSkills": [
      "kubernetes"
    ],
    "experienceAnalysis": "Candidate has 4.0 years of total experience, meeting the required 3 years.",
    "educationAnalysis": "Candidate holds a degree matching the required Bachelor level.",
    "locationAnalysis": "Candidate location 'Bangalore, India' matches job location 'Bangalore, India'.",
    "strengths": [
      "Matches required skill: Java",
      "Matches required skill: Spring boot",
      "Matches preferred skill: Docker",
      "Candidate has 4.0 years of total experience, meeting the required 3 years.",
      "Candidate holds a degree matching the required Bachelor level.",
      "Candidate location 'Bangalore, India' matches job location 'Bangalore, India'."
    ],
    "gaps": [
      "Missing required skill: Postgresql",
      "Missing preferred skill: Kubernetes"
    ]
  },
  "message": "Job match analysis completed successfully"
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
Phase 5 — Job Analysis & Matching
Status: Complete
```

### What's included in Phase 5

- ✅ Reused Phase 2 Candidate `Profile` & Phase 3 `Job` domain entities without modification
- ✅ Deterministic heuristic requirement extraction (`JobRequirementExtractor`)
- ✅ Centralized skill normalization with safe alias resolution (`SkillNormalizationUtil`)
- ✅ Strict non-equivalence checks (`Java` != `JavaScript`, `React` != `React Native`)
- ✅ Skill matching distinguishing required (70%) vs. preferred (30%) skills (`SkillMatcher`)
- ✅ Experience duration calculation & minimum years evaluation (`ExperienceMatcher`)
- ✅ Degree level & field compatibility matching (`EducationMatcher`)
- ✅ Remote and physical location compatibility matching (`LocationMatcher`)
- ✅ Mathematical score calculation with dynamic weight redistribution for `UNKNOWN` dimensions (`MatchScoreCalculator`)
- ✅ `0 <= overallScore <= 100` score bounds and integer rounding
- ✅ Match category classification (`STRONG_MATCH`, `GOOD_MATCH`, `PARTIAL_MATCH`, `WEAK_MATCH`)
- ✅ Data-driven explainability builder for strengths and gaps (`MatchExplanationBuilder`)
- ✅ Dynamic on-demand analysis orchestration service (`JobMatchingService`)
- ✅ `GET /api/jobs/{id}/match` REST endpoint in `JobController`
- ✅ Unit & Integration test suite (`SkillNormalizationUtilTest`, `JobRequirementExtractorTest`, `SkillMatcherTest`, `ExperienceMatcherTest`, `EducationMatcherTest`, `LocationMatcherTest`, `MatchScoreCalculatorTest`, `JobMatchingServiceTest`, `JobMatchingControllerTest`)
- ✅ 101 total passing automated tests across Phase 1, 2, 3, 4, and 5

---

## Future Phases

| Phase | Focus                     | Status       |
|-------|---------------------------|--------------|
| 1     | Backend Foundation        | ✅ Complete  |
| 2     | Candidate Profile         | ✅ Complete  |
| 3     | Job Management            | ✅ Complete  |
| 4     | Job Extraction & Ingestion| ✅ Complete  |
| 5     | Job Analysis & Matching   | ✅ Complete  |
| 6     | Resume Tailoring          | ⏳ Next      |

---

## License

Private project — not for distribution.
